package com.ctoadi.notstore

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ctoadi.notstore.data.AppCount
import com.ctoadi.notstore.data.DateRange
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.data.Overview
import com.ctoadi.notstore.data.Settings
import com.ctoadi.notstore.data.StorageInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** The date filter's current choice. Preset ranges are relative to "now" when applied. */
data class RangeChoice(val preset: Preset, val customFrom: Long = 0, val customUntil: Long = 0) {
    enum class Preset { All, Today, Week, Month, Custom }

    fun toDateRange(): DateRange {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        fun daysAgo(n: Int) = (todayStart.clone() as java.util.Calendar)
            .apply { add(java.util.Calendar.DAY_OF_YEAR, -n) }.timeInMillis
        return when (preset) {
            Preset.All -> DateRange.All
            Preset.Today -> DateRange(todayStart.timeInMillis, null)
            Preset.Week -> DateRange(daysAgo(6), null)
            Preset.Month -> DateRange(daysAgo(29), null)
            Preset.Custom -> DateRange(customFrom, customUntil)
        }
    }

    companion object {
        val All = RangeChoice(Preset.All)
    }
}

data class HistoryState(
    val query: String,
    val items: List<NotificationRecord>,
    val hasMore: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val notstore = app as NotstoreApp
    private val repo = notstore.repository
    private val settingsStore = notstore.settings

    val settings: StateFlow<Settings> = settingsStore.state

    // --- System state, refreshed every time the activity resumes ---

    private val _accessGranted = MutableStateFlow(isAccessGranted())
    val accessGranted: StateFlow<Boolean> = _accessGranted

    private val _batteryUnrestricted = MutableStateFlow(isBatteryUnrestricted())
    val batteryUnrestricted: StateFlow<Boolean> = _batteryUnrestricted

    /** Ticks on resume so "today" rolls over when the app is reopened on a new day. */
    private val clock = MutableStateFlow(0)

    fun refreshSystemState() {
        _accessGranted.value = isAccessGranted()
        _batteryUnrestricted.value = isBatteryUnrestricted()
        clock.value += 1
    }

    private fun isAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(notstore).contains(notstore.packageName)

    private fun isBatteryUnrestricted(): Boolean {
        val pm = notstore.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(notstore.packageName)
    }

    // --- Data ---

    private val reload = combine(repo.changes, clock) { a, b -> a to b }

    val overview: StateFlow<Overview?> = reload
        .mapLatest { repo.overview() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The date filter shared by History and Apps. */
    private val _range = MutableStateFlow(RangeChoice.All)
    val range: StateFlow<RangeChoice> = _range

    fun setRange(choice: RangeChoice) {
        _range.value = choice
        limit.value = PAGE
    }

    val apps: StateFlow<List<AppCount>?> = combine(repo.changes, _range, clock) { _, r, _ -> r }
        .mapLatest { repo.apps(it.toDateRange()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val limit = MutableStateFlow(PAGE)

    val history: StateFlow<HistoryState?> =
        combine(repo.changes, _query, limit, _range, clock) { _, q, l, r, _ -> Triple(q, l, r) }
            .mapLatest { (q, l, r) ->
                // Wait for a pause in typing before searching. A new keystroke cancels this.
                if (q.isNotEmpty()) delay(150)
                val items = repo.list(q, null, r.toDateRange(), l)
                HistoryState(q, items, hasMore = items.size >= l)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        _query.value = value
        limit.value = PAGE
    }

    fun loadMoreHistory() {
        if (history.value?.hasMore == true) limit.value += PAGE
    }

    fun appHistory(pkg: String): Flow<List<NotificationRecord>> =
        combine(repo.changes, _range) { _, r -> r }
            .mapLatest { repo.list("", pkg, it.toDateRange(), APP_LIMIT) }

    val storage: StateFlow<StorageInfo?> = combine(repo.changes, clock) { a, _ -> a }
        .mapLatest { repo.storage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearTemporaryFiles() = repo.clearTemporaryFiles()

    fun delete(record: NotificationRecord) = repo.delete(record.id)
    fun restore(record: NotificationRecord) = repo.restore(record)
    fun deleteAll() = repo.deleteAll()

    // --- Settings ---

    fun setCaptureEnabled(value: Boolean) = settingsStore.setCaptureEnabled(value)
    fun setIncludeOngoing(value: Boolean) = settingsStore.setIncludeOngoing(value)
    fun setExcluded(pkg: String, excluded: Boolean) = settingsStore.setExcluded(pkg, excluded)
    fun dismissBatteryPrompt() = settingsStore.dismissBatteryPrompt()

    fun setRetentionDays(days: Int) {
        settingsStore.setRetentionDays(days)
        repo.pruneNow()
    }

    private companion object {
        const val PAGE = 200
        const val APP_LIMIT = 1000
    }
}
