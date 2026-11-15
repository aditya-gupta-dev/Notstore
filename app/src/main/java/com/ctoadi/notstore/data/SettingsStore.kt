package com.ctoadi.notstore.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val captureEnabled: Boolean,
    val includeOngoing: Boolean,
    /** Days of history to keep. 0 keeps everything. */
    val retentionDays: Int,
    /** Packages the user chose not to capture. */
    val excluded: Set<String>,
    val batteryPromptDismissed: Boolean,
)

/**
 * SharedPreferences-backed settings. The current value is kept in memory, so the listener
 * service can read it on every notification without touching disk.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state

    private fun read() = Settings(
        captureEnabled = prefs.getBoolean(CAPTURE, true),
        includeOngoing = prefs.getBoolean(ONGOING, false),
        retentionDays = prefs.getInt(RETENTION, 0),
        excluded = prefs.getStringSet(EXCLUDED, emptySet())?.toSet() ?: emptySet(),
        batteryPromptDismissed = prefs.getBoolean(BATTERY_DISMISSED, false),
    )

    fun setCaptureEnabled(value: Boolean) {
        prefs.edit().putBoolean(CAPTURE, value).apply()
        _state.value = _state.value.copy(captureEnabled = value)
    }

    fun setIncludeOngoing(value: Boolean) {
        prefs.edit().putBoolean(ONGOING, value).apply()
        _state.value = _state.value.copy(includeOngoing = value)
    }

    fun setRetentionDays(value: Int) {
        prefs.edit().putInt(RETENTION, value).apply()
        _state.value = _state.value.copy(retentionDays = value)
    }

    fun setExcluded(pkg: String, excluded: Boolean) {
        val next = if (excluded) _state.value.excluded + pkg else _state.value.excluded - pkg
        prefs.edit().putStringSet(EXCLUDED, next).apply()
        _state.value = _state.value.copy(excluded = next)
    }

    fun dismissBatteryPrompt() {
        prefs.edit().putBoolean(BATTERY_DISMISSED, true).apply()
        _state.value = _state.value.copy(batteryPromptDismissed = true)
    }

    private companion object {
        const val CAPTURE = "capture_enabled"
        const val ONGOING = "include_ongoing"
        const val RETENTION = "retention_days"
        const val EXCLUDED = "excluded_packages"
        const val BATTERY_DISMISSED = "battery_prompt_dismissed"
    }
}
