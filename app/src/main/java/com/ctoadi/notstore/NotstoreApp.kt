package com.ctoadi.notstore

import android.app.Application
import com.ctoadi.notstore.data.NotificationDb
import com.ctoadi.notstore.data.NotificationRepository
import com.ctoadi.notstore.data.SettingsStore

/** Holds the app-wide singletons. The listener service and the UI share these instances. */
class NotstoreApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: NotificationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        repository = NotificationRepository(this, NotificationDb(this), settings)
    }
}
