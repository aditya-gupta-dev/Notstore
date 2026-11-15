package com.ctoadi.notstore.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Single entry point to the stored notifications. Writes run one at a time on a background
 * thread, in arrival order. [changes] ticks after every write so screens know to reload.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepository(
    context: Context,
    private val db: NotificationDb,
    private val settings: SettingsStore,
) {
    private val appContext = context.applicationContext
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + writer)

    private val _changes = MutableStateFlow(0L)
    val changes: StateFlow<Long> = _changes

    private var lastPruneAt = 0L

    private fun bump() {
        _changes.value += 1
    }

    /** Called by the listener service for every notification that passes its filters. */
    fun capture(c: Captured) {
        scope.launch {
            val last = db.lastForKey(c.key)
            if (last != null) {
                // The system re-posts unchanged notifications fairly often. Save each once.
                if (last.title == c.title && last.body == c.body) {
                    // Same text, but the app may have added a link it didn't have before.
                    if (c.link.isNotEmpty() && c.link != last.link) {
                        db.updateContent(last.id, last.title, last.body, last.postedAt, c.link)
                        bump()
                    }
                    return@launch
                }
                // A progress bar ticking from 10% to 11% replaces its row instead of adding one.
                if (c.updatesInPlace && c.postedAt - last.postedAt < IN_PLACE_WINDOW_MS) {
                    db.updateContent(last.id, c.title, c.body, c.postedAt, c.link)
                    bump()
                    return@launch
                }
            }
            db.insert(
                NotificationRecord(
                    id = 0,
                    key = c.key,
                    pkg = c.pkg,
                    appLabel = resolveLabel(c.pkg),
                    title = c.title,
                    body = c.body,
                    postedAt = c.postedAt,
                    link = c.link,
                )
            )
            pruneIfDue()
            bump()
        }
    }

    private fun resolveLabel(pkg: String): String = try {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun pruneIfDue() {
        val days = settings.state.value.retentionDays
        val now = System.currentTimeMillis()
        if (days <= 0 || now - lastPruneAt < PRUNE_INTERVAL_MS) return
        lastPruneAt = now
        db.deleteOlderThan(now - days * DAY_MS)
    }

    /** Applies the retention setting right away, for example after the user shortens it. */
    fun pruneNow() {
        scope.launch {
            val days = settings.state.value.retentionDays
            if (days > 0) {
                lastPruneAt = System.currentTimeMillis()
                if (db.deleteOlderThan(lastPruneAt - days * DAY_MS) > 0) {
                    db.compact()
                    bump()
                }
            }
        }
    }

    fun delete(id: Long) {
        scope.launch { db.delete(id); bump() }
    }

    /** Puts a deleted record back with its original id (used by Undo). */
    fun restore(record: NotificationRecord) {
        scope.launch { db.insert(record); bump() }
    }

    fun deleteAll() {
        scope.launch {
            db.deleteAll()
            db.compact()
            bump()
        }
    }

    /** Deletes temporary files. Saved notifications are not touched. */
    fun clearTemporaryFiles() {
        scope.launch {
            appContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            bump()
        }
    }

    suspend fun list(query: String, pkg: String?, range: DateRange, limit: Int): List<NotificationRecord> =
        withContext(Dispatchers.IO) { db.list(query, pkg, range, limit) }

    suspend fun apps(range: DateRange): List<AppCount> =
        withContext(Dispatchers.IO) { db.appCounts(range) }

    suspend fun storage(): StorageInfo = withContext(Dispatchers.IO) {
        val dataDir = java.io.File(appContext.applicationInfo.dataDir)
        val databaseBytes = sizeOf(java.io.File(dataDir, "databases"))
        val byText = db.textBytesByApp()
        val textTotal = byText.sumOf { it.second }.coerceAtLeast(1)
        StorageInfo(
            databaseBytes = databaseBytes,
            tempBytes = sizeOf(appContext.cacheDir) + sizeOf(appContext.codeCacheDir),
            settingsBytes = sizeOf(java.io.File(dataDir, "shared_prefs")),
            appBytes = sizeOf(java.io.File(appContext.applicationInfo.sourceDir)),
            notificationCount = byText.sumOf { it.first.count },
            // Split the database's real size in proportion to each app's saved text.
            byApp = byText.map { (app, bytes) ->
                AppStorage(app.pkg, app.label, app.count, databaseBytes * bytes / textTotal)
            },
        )
    }

    private fun sizeOf(file: java.io.File): Long =
        if (!file.exists()) 0
        else if (file.isFile) file.length()
        else file.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    suspend fun overview(): Overview = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startToday = cal.timeInMillis
        // Day boundaries come from the calendar so daylight-saving days are still correct.
        val dayStarts = LongArray(DAYS)
        dayStarts[DAYS - 1] = startToday
        for (i in DAYS - 2 downTo 0) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            dayStarts[i] = cal.timeInMillis
        }

        val heatStart = startToday - (HEATMAP_DAYS - 1) * DAY_MS
        val daily = IntArray(DAYS)
        val hourly = IntArray(24)
        val heat = List(7) { IntArray(24) }
        val slot = Calendar.getInstance()
        for (t in db.timestampsSince(minOf(dayStarts[0], heatStart))) {
            slot.timeInMillis = t
            if (t >= heatStart) {
                // Calendar weekdays run Sunday=1..Saturday=7; rows run Monday=0..Sunday=6.
                val row = (slot.get(Calendar.DAY_OF_WEEK) + 5) % 7
                heat[row][slot.get(Calendar.HOUR_OF_DAY)]++
            }
            if (t < dayStarts[0]) continue
            var day = DAYS - 1
            while (day > 0 && t < dayStarts[day]) day--
            daily[day]++
            if (day == DAYS - 1) hourly[slot.get(Calendar.HOUR_OF_DAY)]++
        }

        Overview(
            total = db.count(),
            todayCount = daily[DAYS - 1],
            hourly = hourly.toList(),
            currentHour = ((now - startToday) / HOUR_MS).toInt().coerceIn(0, 23),
            days = dayStarts.mapIndexed { i, start -> DayBucket(start, daily[i]) },
            topToday = db.appCounts(DateRange(startToday, null)).firstOrNull(),
            topWeek = db.appCounts(DateRange(dayStarts[0], null)).take(TOP_APPS),
            heatmap = heat,
            heatmapDays = HEATMAP_DAYS,
        )
    }

    private companion object {
        const val DAYS = 7
        const val TOP_APPS = 5
        const val HEATMAP_DAYS = 28
        const val HOUR_MS = 60L * 60 * 1000
        const val DAY_MS = 24 * HOUR_MS
        const val PRUNE_INTERVAL_MS = 6 * HOUR_MS
        const val IN_PLACE_WINDOW_MS = 30L * 60 * 1000
    }
}
