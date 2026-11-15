package com.ctoadi.notstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.AppCount
import com.ctoadi.notstore.data.Overview
import com.ctoadi.notstore.data.Settings
import com.ctoadi.notstore.ui.AppIcon
import com.ctoadi.notstore.ui.Banner
import com.ctoadi.notstore.ui.BarChart
import com.ctoadi.notstore.ui.Heatmap
import com.ctoadi.notstore.ui.heatRamp
import com.ctoadi.notstore.ui.NsCard
import com.ctoadi.notstore.ui.ScreenPadding
import com.ctoadi.notstore.ui.ScreenTitle
import com.ctoadi.notstore.ui.SystemActions
import com.ctoadi.notstore.ui.dayLabel
import com.ctoadi.notstore.ui.formatCount
import com.ctoadi.notstore.ui.fullDate
import com.ctoadi.notstore.ui.hourLabel
import com.ctoadi.notstore.ui.notificationCount
import com.ctoadi.notstore.ui.theme.Ns
import com.ctoadi.notstore.ui.weekdayShort
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(
    state: Overview,
    accessGranted: Boolean,
    settings: Settings,
    batteryUnrestricted: Boolean,
    actions: SystemActions,
    onResumeCapture: () -> Unit,
    onDismissBatteryPrompt: () -> Unit,
    onOpenApp: (AppCount) -> Unit,
    onSeeAllApps: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Overview", subtitle = fullDate(context, System.currentTimeMillis()))

        // Only one problem is shown at a time, most serious first.
        when {
            !accessGranted -> Banner(
                title = "Capture is off",
                body = "Notstore no longer has notification access, so new notifications " +
                    "aren't being saved.",
                actionLabel = "Allow access",
                onAction = actions.openNotificationAccess,
            )

            !settings.captureEnabled -> Banner(
                title = "Capture is paused",
                body = "New notifications aren't being saved until you resume.",
                actionLabel = "Resume",
                onAction = onResumeCapture,
                attention = false,
            )

            !batteryUnrestricted && !settings.batteryPromptDismissed -> Banner(
                title = "Keep Notstore running",
                body = "Your phone may stop Notstore in the background to save battery, and " +
                    "notifications would be missed. Allow unrestricted battery use to prevent that.",
                actionLabel = "Allow",
                onAction = actions.requestBatteryExemption,
                attention = false,
                dismissLabel = "Not now",
                onDismiss = onDismissBatteryPrompt,
            )
        }

        TodayCard(state)
        WeekCard(state)
        TopAppsCard(state.topWeek, onOpenApp, onSeeAllApps)
        HeatmapCard(state)
    }
}

/** Weekday names, Monday first, in the device language. */
private fun weekdayNames(long: Boolean): List<String> {
    val symbols = java.text.DateFormatSymbols.getInstance()
    val names = if (long) symbols.weekdays else symbols.shortWeekdays
    // Calendar constants: SUNDAY = 1 ... SATURDAY = 7.
    return listOf(2, 3, 4, 5, 6, 7, 1).map { names[it] }
}

@Composable
private fun HeatmapCard(state: Overview) {
    val context = LocalContext.current
    val c = Ns.colors
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val short = remember { weekdayNames(long = false) }
    val long = remember { weekdayNames(long = true) }
    val weeks = state.heatmapDays / 7

    val total = state.heatmap.sumOf { it.sum() }
    var peak = 0 to 0
    for (r in 0 until 7) for (h in 0 until 24) {
        if (state.heatmap[r][h] > state.heatmap[peak.first][peak.second]) peak = r to h
    }
    val headline = if (total == 0) "Not enough data yet"
    else "${long[peak.first]}s around ${hourLabel(peak.second, context)}"
    val summary = if (total == 0) "This fills in over the coming weeks."
    else "Busiest time over the last $weeks weeks"
    val detail = selected?.let { (r, h) ->
        "${long[r]}s, ${hourLabel(h, context)} to ${hourLabel(h + 1, context)}: " +
            notificationCount(state.heatmap[r][h])
    }

    NsCard {
        CardLabel("When notifications arrive")
        Spacer(Modifier.height(2.dp))
        Text(headline, style = Ns.type.title, modifier = Modifier.semantics { heading() })
        Text(
            text = detail ?: summary,
            style = Ns.type.subhead,
            color = if (detail != null) c.accent else c.secondary,
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.width(LabelColumn)) {
                short.forEachIndexed { i, name ->
                    Box(
                        Modifier.height(HeatCell + if (i < 6) HeatGap else 0.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Text(
                            name,
                            style = Ns.type.caption,
                            color = c.secondary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            Heatmap(
                grid = state.heatmap,
                selected = selected,
                onSelect = { selected = it },
                cellHeight = HeatCell,
                gap = HeatGap,
                description = "Heatmap of notifications by weekday and hour over the last " +
                    "$weeks weeks. $headline. $summary.",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(start = LabelColumn)) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(
                    text = hourLabel(hour, context),
                    style = Ns.type.caption,
                    color = c.secondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Scale legend: the same five steps the cells use.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = Ns.type.caption, color = c.secondary)
            Spacer(Modifier.width(6.dp))
            heatRamp(c.track, c.accent).forEach { color ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = Ns.type.caption, color = c.secondary)
        }
    }
}

private val LabelColumn = 40.dp
private val HeatCell = 18.dp
private val HeatGap = 2.dp

@Composable
private fun CardLabel(text: String) {
    Text(text, style = Ns.type.caption, color = Ns.colors.secondary)
}

@Composable
private fun TodayCard(state: Overview) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Int?>(null) }
    val peakHour = state.hourly.indices.maxByOrNull { state.hourly[it] } ?: 0

    val headline = if (state.todayCount == 0) "No notifications yet today"
    else "${notificationCount(state.todayCount)} today"

    val summary = when {
        state.todayCount == 0 -> "They'll appear here as they arrive."
        else -> buildString {
            append("Busiest at ").append(hourLabel(peakHour, context))
            state.topToday?.let { append(" · Most from ").append(it.label) }
        }
    }
    val detail = selected?.let { h ->
        "${hourLabel(h, context)} to ${hourLabel(h + 1, context)}: " +
            notificationCount(state.hourly[h])
    }

    NsCard {
        CardLabel("Today, hour by hour")
        Spacer(Modifier.height(2.dp))
        Text(headline, style = Ns.type.title, modifier = Modifier.semantics { heading() })
        Text(
            text = detail ?: summary,
            style = Ns.type.subhead,
            color = if (detail != null) Ns.colors.accent else Ns.colors.secondary,
        )
        Spacer(Modifier.height(20.dp))
        BarChart(
            values = state.hourly,
            selected = selected,
            onSelect = { selected = it },
            futureFrom = state.currentHour + 1,
            description = "Bar chart of notifications for each hour of today. $headline. $summary",
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(
                    text = hourLabel(hour, context),
                    style = Ns.type.caption,
                    color = Ns.colors.secondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekCard(state: Overview) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Int?>(null) }
    val weekTotal = state.days.sumOf { it.count }
    val average = (weekTotal / state.days.size.toFloat()).roundToInt()
    val todayStart = state.days.last().start

    val headline = when {
        weekTotal == 0 -> "Nothing in the last 7 days"
        average < 1 -> "Fewer than 1 a day"
        else -> "About ${formatCount(average)} a day"
    }
    val summary = if (weekTotal == 0) "Days fill in as notifications arrive."
    else "${notificationCount(weekTotal)} in the last 7 days"
    val detail = selected?.let { i ->
        val day = state.days[i]
        "${dayLabel(context, day.start, todayStart)}: ${notificationCount(day.count)}"
    }

    NsCard {
        CardLabel("Last 7 days")
        Spacer(Modifier.height(2.dp))
        Text(headline, style = Ns.type.title, modifier = Modifier.semantics { heading() })
        Text(
            text = detail ?: summary,
            style = Ns.type.subhead,
            color = if (detail != null) Ns.colors.accent else Ns.colors.secondary,
        )
        Spacer(Modifier.height(20.dp))
        BarChart(
            values = state.days.map { it.count },
            selected = selected,
            onSelect = { selected = it },
            description = "Bar chart of notifications for each of the last 7 days. " +
                "$headline. $summary",
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            state.days.forEachIndexed { i, day ->
                val isToday = i == state.days.lastIndex
                Text(
                    text = weekdayShort(day.start),
                    style = Ns.type.caption,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) Ns.colors.ink else Ns.colors.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TopAppsCard(
    apps: List<AppCount>,
    onOpenApp: (AppCount) -> Unit,
    onSeeAllApps: () -> Unit,
) {
    val c = Ns.colors
    NsCard(padding = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(top = 10.dp)) {
                CardLabel("Last 7 days")
                Text(
                    "Most active apps",
                    style = Ns.type.title,
                    modifier = Modifier.semantics { heading() },
                )
            }
            TextButton(onClick = onSeeAllApps) {
                Text("All apps", style = Ns.type.headline, color = c.accent)
            }
        }
        if (apps.isEmpty()) {
            Text(
                "No apps have sent notifications in the last 7 days.",
                style = Ns.type.subhead,
                color = c.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
            return@NsCard
        }
        Spacer(Modifier.height(6.dp))
        val max = apps.maxOf { it.count }.coerceAtLeast(1)
        apps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Show notifications from ${app.label}") {
                        onOpenApp(app)
                    }
                    .defaultMinSize(minHeight = 60.dp)
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app.pkg, app.label, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = Ns.type.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    // Length shows this app's share relative to the busiest app.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.track)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(app.count / max.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(c.accent)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
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
        Spacer(Modifier.height(8.dp))
    }
}
