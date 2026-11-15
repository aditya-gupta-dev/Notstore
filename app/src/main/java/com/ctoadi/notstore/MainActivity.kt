package com.ctoadi.notstore

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ctoadi.notstore.service.NotificationCaptureService
import com.ctoadi.notstore.ui.NotstoreRoot
import com.ctoadi.notstore.ui.SystemActions
import com.ctoadi.notstore.ui.theme.NotstoreTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val actions = SystemActions(
            openNotificationAccess = ::openNotificationAccess,
            requestBatteryExemption = ::requestBatteryExemption,
            openAppInfo = ::openAppInfo,
            openAutostart = autostartIntent()?.let { intent -> { launch(intent) } },
            openApp = { pkg -> packageManager.getLaunchIntentForPackage(pkg)?.let(::launch) },
            openLink = { url -> launch(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        )

        setContent {
            NotstoreTheme {
                NotstoreRoot(viewModel, actions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemState()
        // If the system dropped the listener binding, ask for it back.
        if (viewModel.accessGranted.value) {
            runCatching {
                NotificationListenerService.requestRebind(
                    ComponentName(this, NotificationCaptureService::class.java)
                )
            }
        }
    }

    private fun launch(intent: Intent) {
        runCatching { startActivity(intent) }
    }

    private fun openNotificationAccess() {
        launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openAppInfo() {
        launch(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        )
    }

    private fun requestBatteryExemption() {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        if (runCatching { startActivity(direct) }.isFailure) {
            launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    /** Xiaomi, Redmi and POCO phones need a separate "Autostart" switch for background apps. */
    private fun autostartIntent(): Intent? {
        val maker = Build.MANUFACTURER.lowercase()
        if (maker !in setOf("xiaomi", "redmi", "poco")) return null
        val intent = Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            )
        )
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }
}
