package com.ctoadi.notstore.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ctoadi.notstore.RangeChoice
import com.ctoadi.notstore.ui.RangeFilter
import com.ctoadi.notstore.ui.prefetchIcons
import com.ctoadi.notstore.ui.rangePhrase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.AppCount
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.AppIcon
import com.ctoadi.notstore.ui.EmptyState
import com.ctoadi.notstore.ui.GroupRow
import com.ctoadi.notstore.ui.NotificationList
import com.ctoadi.notstore.ui.NsCard
import com.ctoadi.notstore.ui.ScreenPadding
import com.ctoadi.notstore.ui.ScreenTitle
import com.ctoadi.notstore.ui.formatCount
import com.ctoadi.notstore.ui.notificationCount
import com.ctoadi.notstore.ui.relativeTime
import com.ctoadi.notstore.ui.rowPosition
import com.ctoadi.notstore.ui.theme.Ns

@Composable
fun AppsScreen(
    apps: List<AppCount>?,
    excluded: Set<String>,
    range: RangeChoice,
    onRangeChange: (RangeChoice) -> Unit,
    onOpen: (AppCount) -> Unit,
) {
    val c = Ns.colors
    val context = LocalContext.current
    // Work out each row's subtitle once per data change, not on every frame of a scroll.
    val subtitles = remember(apps, excluded) {
        apps.orEmpty().associate { app ->
            app.pkg to if (app.pkg in excluded) "Not being captured" else "Last: ${relativeTime(app.lastAt)}"
        }
    }
    LaunchedEffect(apps) { prefetchIcons(context, apps.orEmpty().map { it.pkg }) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
    ) {
        item(key = "title") {
            ScreenTitle(
                title = "Apps",
                subtitle = when {
                    apps.isNullOrEmpty() -> null
                    apps.size == 1 -> "1 app"
                    else -> "${formatCount(apps.size)} apps"
                },
            )
            Spacer(Modifier.height(8.dp))
            RangeFilter(range, onRangeChange)
            Spacer(Modifier.height(12.dp))
        }
        if (apps != null && apps.isEmpty()) {
            item(key = "empty") {
                if (range.preset == RangeChoice.Preset.All) {
                    EmptyState(
                        title = "No apps yet",
                        body = "Each app appears here after its first notification.",
                    )
                } else {
                    EmptyState(
                        title = "No notifications${rangePhrase(range)}",
                        body = "Choose a longer range to see more.",
                    )
                }
            }
        }
        itemsIndexed(apps.orEmpty(), key = { _, app -> app.pkg }) { index, app ->
            GroupRow(
                position = rowPosition(index, apps.orEmpty().size),
                onClick = { onOpen(app) },
                dividerInset = 68.dp,
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.pkg, app.label, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.label,
                            style = Ns.type.body,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitles[app.pkg].orEmpty(),
                            style = Ns.type.subhead,
                            color = c.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatCount(app.count),
                        style = Ns.type.headline.copy(fontFeatureSettings = Ns.type.TABULAR),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = c.secondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** One app's notifications, plus the switch that stops capturing it. */
@Composable
fun AppDetailScreen(
    pkg: String,
    label: String,
    items: List<NotificationRecord>?,
    captured: Boolean,
    onCapturedChange: (Boolean) -> Unit,
    range: RangeChoice,
    onRangeChange: (RangeChoice) -> Unit,
    onBack: () -> Unit,
    onOpen: (NotificationRecord) -> Unit,
) {
    val c = Ns.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = ScreenPadding, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to apps",
                    tint = c.ink,
                )
            }
            Spacer(Modifier.width(4.dp))
            AppIcon(pkg, label, 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = Ns.type.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        NotificationList(
            items = items.orEmpty(),
            showApp = false,
            onOpen = onOpen,
            modifier = Modifier.fillMaxSize(),
            header = {
                item(key = "capture") {
                    Spacer(Modifier.height(12.dp))
                    NsCard(padding = 0.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = captured,
                                    role = Role.Switch,
                                    onValueChange = onCapturedChange,
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Capture this app", style = Ns.type.body)
                                if (!captured) {
                                    Text(
                                        "Saved ones stay in History.",
                                        style = Ns.type.subhead,
                                        color = c.secondary,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(checked = captured, onCheckedChange = null)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RangeFilter(range, onRangeChange)
                    if (items != null) {
                        Text(
                            text = if (items.isEmpty()) "Nothing saved from this app${rangePhrase(range)}."
                            else "${notificationCount(items.size)} saved",
                            style = Ns.type.subhead,
                            color = c.secondary,
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                        )
                    }
                }
            },
        )
    }
}
