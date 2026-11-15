package com.ctoadi.notstore.service

import android.app.PendingIntent
import android.util.LruCache

/**
 * The tap action ("content intent") of recent notifications, by status-bar key.
 *
 * Android doesn't let other apps read where a notification's tap action leads, so it can't be
 * saved to disk. What we can do is keep the action itself while this process is alive and fire
 * it later, which opens the exact screen the notification pointed to. It's lost when the
 * process restarts, and stops working once the posting app cancels it.
 */
object LiveIntents {
    private val cache = LruCache<String, PendingIntent>(500)

    fun put(key: String, intent: PendingIntent?) {
        if (intent != null) cache.put(key, intent)
    }

    fun get(key: String): PendingIntent? = cache.get(key)
}
