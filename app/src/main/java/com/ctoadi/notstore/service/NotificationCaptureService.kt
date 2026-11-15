package com.ctoadi.notstore.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Patterns
import com.ctoadi.notstore.NotstoreApp
import com.ctoadi.notstore.data.Captured

/**
 * Receives every notification posted on the device.
 *
 * The system binds this service once the user grants notification access and keeps it bound
 * whether or not the app is open, including after a reboot. No foreground service is needed.
 */
class NotificationCaptureService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        // After a restart, recover the tap actions of notifications that are still showing.
        runCatching {
            activeNotifications?.forEach { LiveIntents.put(it.key, it.notification?.contentIntent) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return

        val app = application as NotstoreApp
        val settings = app.settings.state.value
        if (!settings.captureEnabled || sbn.packageName in settings.excluded) return

        val notification = sbn.notification ?: return
        // A group summary ("5 new messages") only repeats what its child notifications say.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (sbn.isOngoing && !settings.includeOngoing) return

        val extras = notification.extras ?: return
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
            ?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        var body = if (bigText.length > text.length) bigText else text
        if (body.isEmpty()) {
            body = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.joinToString("\n") { it.toString().trim() }.orEmpty()
        }
        if (title.isEmpty() && body.isEmpty()) return

        LiveIntents.put(sbn.key, notification.contentIntent)

        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
            extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        app.repository.capture(
            Captured(
                key = sbn.key,
                pkg = sbn.packageName,
                title = title,
                body = body,
                postedAt = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis(),
                link = findLink(extras, title, body),
                updatesInPlace = hasProgress || sbn.isOngoing,
            )
        )
    }

    /** The first web address in the notification's text, if any. */
    private fun findLink(extras: android.os.Bundle, title: String, body: String): String {
        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val info = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
        for (text in listOf(body, title, sub, info)) {
            val m = Patterns.WEB_URL.matcher(text)
            while (m.find()) {
                val url = m.group().trimEnd('.', ',', ')', '!', '?')
                // WEB_URL also matches bare words like "file.txt"; keep real links only.
                if (url.startsWith("http://", true) || url.startsWith("https://", true) ||
                    url.startsWith("www.", true)
                ) {
                    return if (url.startsWith("www.", true)) "https://$url" else url
                }
            }
        }
        return ""
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Ask the system to reconnect if it dropped the binding.
        runCatching { requestRebind(ComponentName(this, NotificationCaptureService::class.java)) }
    }
}
