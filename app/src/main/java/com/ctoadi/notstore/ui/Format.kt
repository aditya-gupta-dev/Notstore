package com.ctoadi.notstore.ui

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatCount(n: Int): String = NumberFormat.getIntegerInstance().format(n)

fun notificationCount(n: Int): String =
    if (n == 1) "1 notification" else "${formatCount(n)} notifications"

/** "9 PM" or "21:00", following the device's 12/24-hour setting. */
fun hourLabel(hour: Int, context: Context): String {
    val h = hour % 24
    if (DateFormat.is24HourFormat(context)) return String.format(Locale.getDefault(), "%02d:00", h)
    val twelve = if (h % 12 == 0) 12 else h % 12
    return "$twelve ${if (h < 12) "AM" else "PM"}"
}

fun timeOfDay(context: Context, time: Long): String =
    DateFormat.getTimeFormat(context).format(Date(time))

fun fullDate(context: Context, time: Long): String =
    DateUtils.formatDateTime(
        context, time, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE
    )

fun fullDateTime(context: Context, time: Long): String =
    DateUtils.formatDateTime(
        context, time,
        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
    )

fun relativeTime(time: Long): String {
    val now = System.currentTimeMillis()
    if (now - time < DateUtils.MINUTE_IN_MILLIS) return "just now"
    return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

fun weekdayShort(time: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(time))

fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** "Today", "Yesterday", or a full date for anything older. */
fun dayLabel(context: Context, dayStart: Long, todayStart: Long): String {
    val yesterdayStart = startOfDay(todayStart - 1)
    return when (dayStart) {
        todayStart -> "Today"
        yesterdayStart -> "Yesterday"
        else -> fullDate(context, dayStart)
    }
}
