package com.ctoadi.notstore.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ctoadi.notstore.MainViewModel
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.screens.AppDetailScreen
import com.ctoadi.notstore.ui.screens.AppsScreen
import com.ctoadi.notstore.ui.screens.CacheScreen
import com.ctoadi.notstore.ui.screens.HistoryScreen
import com.ctoadi.notstore.ui.screens.OverviewScreen
import com.ctoadi.notstore.ui.screens.SettingsScreen
import com.ctoadi.notstore.ui.screens.SetupScreen
import com.ctoadi.notstore.ui.theme.Ns
import kotlinx.coroutines.launch

/** Things only the activity can do: open system settings screens and other apps. */
class SystemActions(
    val openNotificationAccess: () -> Unit,
    val requestBatteryExemption: () -> Unit,
    val openAppInfo: () -> Unit,
    /** Null on phones that have no separate autostart setting. */
    val openAutostart: (() -> Unit)?,
    val openApp: (String) -> Unit,
    val openLink: (String) -> Unit,
)

private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Overview("Overview", Icons.Outlined.Insights, Icons.Filled.Insights),
    History("History", Icons.Outlined.History, Icons.Filled.History),
    Apps("Apps", Icons.Outlined.Apps, Icons.Filled.Apps),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

@Composable
fun NotstoreRoot(vm: MainViewModel, actions: SystemActions) {
    val c = Ns.colors
    val overview by vm.overview.collectAsStateWithLifecycle()
    val accessGranted by vm.accessGranted.collectAsStateWithLifecycle()
    val batteryUnrestricted by vm.batteryUnrestricted.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val range by vm.range.collectAsStateWithLifecycle()

    val loaded = overview
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(c.bg))
        return
    }
    // First run. Once there is history, it stays reachable even if access is later revoked.
    if (!accessGranted && loaded.total == 0) {
        SetupScreen(actions)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Overview) }
    var detailPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var detailLabel by rememberSaveable { mutableStateOf("") }
    var cachePage by rememberSaveable { mutableStateOf(false) }
    var openRecord by remember { mutableStateOf<NotificationRecord?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showingDetail = (tab == Tab.Apps && detailPkg != null) || (tab == Tab.Settings && cachePage)
    BackHandler(enabled = !showingDetail && tab != Tab.Overview) { tab = Tab.Overview }
    BackHandler(enabled = showingDetail) {
        if (tab == Tab.Apps) detailPkg = null else cachePage = false
    }

    fun openAppDetail(pkg: String, label: String) {
        detailPkg = pkg
        detailLabel = label
        tab = Tab.Apps
    }

    Scaffold(
        containerColor = c.bg,
        contentColor = c.ink,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                Hairline()
                NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
                    Tab.entries.forEach { item ->
                        val selected = tab == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // Tapping the current tab again returns to its top level.
                                if (item == Tab.Apps && tab == Tab.Apps) detailPkg = null
                                if (item == Tab.Settings && tab == Tab.Settings) cachePage = false
                                tab = item
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(item.label, style = Ns.type.caption, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.accent,
                                selectedTextColor = c.accent,
                                indicatorColor = c.accentSoft,
                                unselectedIconColor = c.secondary,
                                unselectedTextColor = c.secondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // On tablets and unfolded phones, content keeps a readable width instead of stretching.
            Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                when (tab) {
                    Tab.Overview -> OverviewScreen(
                        state = loaded,
                        accessGranted = accessGranted,
                        settings = settings,
                        batteryUnrestricted = batteryUnrestricted,
                        actions = actions,
                        onResumeCapture = { vm.setCaptureEnabled(true) },
                        onDismissBatteryPrompt = vm::dismissBatteryPrompt,
                        onOpenApp = { openAppDetail(it.pkg, it.label) },
                        onSeeAllApps = {
                            detailPkg = null
                            tab = Tab.Apps
                        },
                    )

                    Tab.History -> {
                        val history by vm.history.collectAsStateWithLifecycle()
                        HistoryScreen(
                            state = history,
                            initialQuery = vm.query.value,
                            onQueryChange = vm::setQuery,
                            range = range,
                            onRangeChange = vm::setRange,
                            onOpen = { openRecord = it },
                            onLoadMore = vm::loadMoreHistory,
                        )
                    }

                    Tab.Apps -> {
                        val pkg = detailPkg
                        if (pkg == null) {
                            val apps by vm.apps.collectAsStateWithLifecycle()
                            AppsScreen(
                                apps = apps,
                                excluded = settings.excluded,
                                range = range,
                                onRangeChange = vm::setRange,
                                onOpen = { openAppDetail(it.pkg, it.label) },
                            )
                        } else {
                            val items by remember(pkg) { vm.appHistory(pkg) }
                                .collectAsStateWithLifecycle(initialValue = null)
                            AppDetailScreen(
                                pkg = pkg,
                                label = detailLabel,
                                items = items,
                                captured = pkg !in settings.excluded,
                                onCapturedChange = { vm.setExcluded(pkg, !it) },
                                range = range,
                                onRangeChange = vm::setRange,
                                onBack = { detailPkg = null },
                                onOpen = { openRecord = it },
                            )
                        }
                    }

                    Tab.Settings -> {
                        val storage by vm.storage.collectAsStateWithLifecycle()
                        if (cachePage) {
                            CacheScreen(
                                info = storage,
                                onBack = { cachePage = false },
                                onClearTemporary = vm::clearTemporaryFiles,
                            )
                        } else {
                            SettingsScreen(
                                settings = settings,
                                accessGranted = accessGranted,
                                batteryUnrestricted = batteryUnrestricted,
                                total = loaded.total,
                                cacheBytes = storage?.cacheBytes,
                                actions = actions,
                                onCaptureEnabled = vm::setCaptureEnabled,
                                onIncludeOngoing = vm::setIncludeOngoing,
                                onRetentionDays = vm::setRetentionDays,
                                onDeleteAll = vm::deleteAll,
                                onOpenCache = { cachePage = true },
                            )
                        }
                    }
                }
            }
        }
    }

    openRecord?.let { record ->
        NotificationSheet(
            record = record,
            onDismiss = { openRecord = null },
            onOpenApp = actions.openApp,
            onOpenLink = actions.openLink,
            onDelete = { deleted ->
                openRecord = null
                vm.delete(deleted)
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "Notification deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.restore(deleted)
                }
            },
        )
    }
}
