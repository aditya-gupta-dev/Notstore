package com.ctoadi.notstore.data

/** One saved notification. */
data class NotificationRecord(
    val id: Long,
    val key: String,
    val pkg: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    /** A web address or app link found in the notification, or empty. */
    val link: String = "",
)

/** What the listener service hands to the repository. */
data class Captured(
    val key: String,
    val pkg: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val link: String,
    /** True for progress bars and ongoing notifications, which update in place instead of adding rows. */
    val updatesInPlace: Boolean,
)

data class AppCount(
    val pkg: String,
    val label: String,
    val count: Int,
    val lastAt: Long,
)

data class DayBucket(
    val start: Long,
    val count: Int,
)

/** A span of time to filter by. Null ends are open. [until] is exclusive. */
data class DateRange(val from: Long?, val until: Long?) {
    companion object {
        val All = DateRange(null, null)
    }
}

/** Everything the Overview screen shows, computed in one pass. */
data class Overview(
    val total: Int,
    val todayCount: Int,
    val hourly: List<Int>,
    val currentHour: Int,
    val days: List<DayBucket>,
    val topToday: AppCount?,
    val topWeek: List<AppCount>,
    /** Counts by weekday and hour over [heatmapDays] days. Row 0 is Monday. */
    val heatmap: List<IntArray>,
    val heatmapDays: Int,
)

/** One app's share of the saved notifications. [bytes] is estimated from its saved text. */
data class AppStorage(
    val pkg: String,
    val label: String,
    val count: Int,
    val bytes: Long,
)

/** Where Notstore's disk space goes. */
data class StorageInfo(
    /** The notification database, including its write-ahead log. */
    val databaseBytes: Long,
    /** Files the system may also clear on its own when space runs low. */
    val tempBytes: Long,
    val settingsBytes: Long,
    /** The installed app itself. Not part of the cache; shown for context. */
    val appBytes: Long,
    val notificationCount: Int,
    val byApp: List<AppStorage>,
) {
    val cacheBytes: Long get() = databaseBytes + tempBytes + settingsBytes
}
