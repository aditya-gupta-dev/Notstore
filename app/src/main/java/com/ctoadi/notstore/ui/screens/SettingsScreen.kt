package com.ctoadi.notstore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.text.format.Formatter
import com.ctoadi.notstore.ui.AboutButton
import com.ctoadi.notstore.ui.AboutDialog
import com.ctoadi.notstore.data.Settings
import com.ctoadi.notstore.ui.GroupRow
import com.ctoadi.notstore.ui.RowPosition
import com.ctoadi.notstore.ui.ScreenPadding
import com.ctoadi.notstore.ui.ScreenTitle
import com.ctoadi.notstore.ui.SystemActions
import com.ctoadi.notstore.ui.notificationCount
import com.ctoadi.notstore.ui.theme.Ns

private val retentionOptions = listOf(
    0 to "Forever",
    90 to "90 days",
    30 to "30 days",
    7 to "7 days",
)

private fun retentionLabel(days: Int): String =
    retentionOptions.firstOrNull { it.first == days }?.second ?: "$days days"

@Composable
fun SettingsScreen(
    settings: Settings,
    accessGranted: Boolean,
    batteryUnrestricted: Boolean,
    total: Int,
    cacheBytes: Long?,
    actions: SystemActions,
    onCaptureEnabled: (Boolean) -> Unit,
    onIncludeOngoing: (Boolean) -> Unit,
    onRetentionDays: (Int) -> Unit,
    onDeleteAll: () -> Unit,
    onOpenCache: () -> Unit,
) {
    val c = Ns.colors
    val context = LocalContext.current
    var showRetention by rememberSaveable { mutableStateOf(false) }
    var showDeleteAll by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = 24.dp),
    ) {
        ScreenTitle("Settings")

        SectionHeader("Capture")
        ToggleRow(
            position = RowPosition.First,
            title = "Capture notifications",
            checked = settings.captureEnabled,
            onCheckedChange = onCaptureEnabled,
        )
        ToggleRow(
            position = RowPosition.Last,
            title = "Include ongoing",
            subtitle = "Music, downloads, calls",
            checked = settings.includeOngoing,
            onCheckedChange = onIncludeOngoing,
        )

        SectionHeader("Background")
        val showAutostart = actions.openAutostart != null
        LinkRow(
            position = RowPosition.First,
            title = "Notification access",
            value = if (accessGranted) "On" else "Off",
            valueColor = if (accessGranted) c.secondary else c.signal,
            onClick = actions.openNotificationAccess,
        )
        LinkRow(
            position = if (showAutostart) RowPosition.Middle else RowPosition.Last,
            title = "Battery use",
            value = if (batteryUnrestricted) "Unrestricted" else "Restricted",
            valueColor = if (batteryUnrestricted) c.secondary else c.signal,
            onClick = if (batteryUnrestricted) actions.openAppInfo else actions.requestBatteryExemption,
        )
        if (actions.openAutostart != null) {
            LinkRow(
                position = RowPosition.Last,
                title = "Autostart",
                value = "",
                onClick = actions.openAutostart,
            )
        }

        SectionHeader("Cache")
        LinkRow(
            position = RowPosition.First,
            title = "Cache size",
            value = cacheBytes?.let { Formatter.formatShortFileSize(context, it) }.orEmpty(),
            onClick = onOpenCache,
        )
        LinkRow(
            position = RowPosition.Middle,
            title = "Keep cache for",
            value = retentionLabel(settings.retentionDays),
            onClick = { showRetention = true },
        )
        GroupRow(
            position = RowPosition.Last,
            onClick = if (total > 0) ({ showDeleteAll = true }) else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Delete cache\u2026",
                    style = Ns.type.body,
                    color = if (total > 0) c.danger else c.secondary,
                )
            }
        }
        SectionFooter("${notificationCount(total)} stored on this phone only")

        Spacer(Modifier.height(28.dp))
        AboutButton(
            onOpen = { showAbout = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false }, onOpenLink = actions.openLink)

    if (showRetention) {
        AlertDialog(
            onDismissRequest = { showRetention = false },
            title = { Text("Keep notifications for") },
            text = {
                Column {
                    retentionOptions.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.retentionDays == days,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onRetentionDays(days)
                                        showRetention = false
                                    },
                                )
                                .defaultMinSize(minHeight = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.retentionDays == days, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = Ns.type.body)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetention = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete cache?") },
            text = {
                Text(
                    "All ${notificationCount(total)} saved on this phone will be permanently " +
                        "deleted. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteAll = false; onDeleteAll() }) {
                    Text("Delete", color = c.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = Ns.type.subhead,
        fontWeight = FontWeight.SemiBold,
        color = Ns.colors.secondary,
        modifier = Modifier
            .padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        style = Ns.type.subhead,
        color = Ns.colors.secondary,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    position: RowPosition,
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    GroupRow(position = position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Ns.type.body)
                if (subtitle != null) {
                    Text(subtitle, style = Ns.type.subhead, color = Ns.colors.secondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun LinkRow(
    position: RowPosition,
    title: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = Ns.colors.secondary,
) {
    GroupRow(position = position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .defaultMinSize(minHeight = 56.dp)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = Ns.type.body, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Text(value, style = Ns.type.body, color = valueColor)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Ns.colors.secondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
