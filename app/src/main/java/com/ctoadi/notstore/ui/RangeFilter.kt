package com.ctoadi.notstore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.RangeChoice
import com.ctoadi.notstore.RangeChoice.Preset
import com.ctoadi.notstore.ui.theme.Ns
import java.util.Calendar
import java.util.TimeZone

private val presets = listOf(
    Preset.All to "All",
    Preset.Today to "Today",
    Preset.Week to "7 days",
    Preset.Month to "30 days",
)

private val TrackShape = RoundedCornerShape(12.dp)
private val SegmentShape = RoundedCornerShape(9.dp)

/**
 * Date filter: a segmented control for the common ranges, plus a calendar button for a
 * custom range. The calendar is a separate button because it performs an action (opening a
 * picker), while the segments only show which range is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeFilter(
    choice: RangeChoice,
    onChange: (RangeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ns.colors
    val context = LocalContext.current
    var picking by rememberSaveable { mutableStateOf(false) }
    val custom = choice.preset == Preset.Custom

    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(TrackShape)
                    .background(c.card)
                    .padding(3.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                presets.forEach { (preset, label) ->
                    val selected = choice.preset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(SegmentShape)
                            .background(if (selected) c.accentSoft else c.card)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onChange(RangeChoice(preset)) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = Ns.type.subhead,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) c.accent else c.ink,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { picking = true }) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "Choose dates",
                    tint = if (custom) c.accent else c.secondary,
                )
            }
        }

        if (custom) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(TrackShape)
                    .background(c.accentSoft)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = customLabel(context, choice),
                    style = Ns.type.subhead,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent,
                )
                IconButton(onClick = { onChange(RangeChoice.All) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear dates",
                        tint = c.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (picking) {
        val todayUtc = localDayToUtc(System.currentTimeMillis())
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = if (custom) localDayToUtc(choice.customFrom) else null,
            initialSelectedEndDateMillis = if (custom) localDayToUtc(choice.customUntil - 1) else null,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis
                        if (start != null) {
                            val end = state.selectedEndDateMillis ?: start
                            onChange(
                                RangeChoice(
                                    Preset.Custom,
                                    customFrom = utcDayToLocalStart(start),
                                    customUntil = utcDayToLocalStart(end, plusDays = 1),
                                )
                            )
                        }
                        picking = false
                    },
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    }
}

private fun customLabel(context: android.content.Context, choice: RangeChoice): String {
    val flags = android.text.format.DateUtils.FORMAT_SHOW_DATE or
        android.text.format.DateUtils.FORMAT_ABBREV_MONTH
    val first = android.text.format.DateUtils.formatDateTime(context, choice.customFrom, flags)
    val lastDay = choice.customUntil - 1
    return if (startOfDay(lastDay) == choice.customFrom) first
    else first + " – " + android.text.format.DateUtils.formatDateTime(context, lastDay, flags)
}

/** The date picker works in UTC midnights; these convert to and from local days. */
private fun localDayToUtc(localTime: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localTime }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcDayToLocalStart(utcMidnight: Long, plusDays: Int = 0): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnight }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        add(Calendar.DAY_OF_YEAR, plusDays)
    }.timeInMillis
}

/** Describes the active range in a sentence, for empty states. */
fun rangePhrase(choice: RangeChoice): String = when (choice.preset) {
    Preset.All -> ""
    Preset.Today -> " today"
    Preset.Week -> " in the last 7 days"
    Preset.Month -> " in the last 30 days"
    Preset.Custom -> " in these dates"
}
