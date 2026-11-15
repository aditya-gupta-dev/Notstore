package com.ctoadi.notstore.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.theme.Ns

private sealed interface ListEntry {
    data class DayHeader(val dayStart: Long, val label: String) : ListEntry
    data class Item(
        val record: NotificationRecord,
        val time: String,
        val position: RowPosition,
    ) : ListEntry
}

/** Splits a newest-first list into days, each rendered as one rounded group under a heading. */
private fun buildEntries(context: Context, items: List<NotificationRecord>): List<ListEntry> {
    val todayStart = startOfDay(System.currentTimeMillis())
    // One formatter for the whole list instead of one per row.
    val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
    val out = ArrayList<ListEntry>(items.size + 16)
    var i = 0
    while (i < items.size) {
        val dayStart = startOfDay(items[i].postedAt)
        var end = i
        while (end < items.size && items[end].postedAt >= dayStart) end++
        out += ListEntry.DayHeader(dayStart, dayLabel(context, dayStart, todayStart))
        for (j in i until end) {
            out += ListEntry.Item(
                record = items[j],
                time = timeFormat.format(java.util.Date(items[j].postedAt)),
                position = rowPosition(j - i, end - i),
            )
        }
        i = end
    }
    return out
}

/**
 * Notifications grouped by day.
 *
 * @param showApp show each row's app icon and name; off when the whole list is one app.
 * @param header extra items placed above the list (titles, controls).
 * @param onNearEnd called when the user scrolls close to the end, to load more.
 */
@Composable
fun NotificationList(
    items: List<NotificationRecord>,
    showApp: Boolean,
    onOpen: (NotificationRecord) -> Unit,
    modifier: Modifier = Modifier,
    onNearEnd: () -> Unit = {},
    header: LazyListScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val entries = remember(items) { buildEntries(context, items) }
    val listState = rememberLazyListState()
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)

    LaunchedEffect(items) {
        prefetchIcons(context, items.asSequence().map { it.pkg }.distinct().toList())
    }

    LaunchedEffect(listState, entries.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (entries.isNotEmpty() && lastVisible >= entries.size - 40) currentOnNearEnd()
            }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = ScreenPadding, end = ScreenPadding, bottom = 24.dp
        ),
    ) {
        header()
        items(
            items = entries,
            key = { entry ->
                when (entry) {
                    is ListEntry.DayHeader -> "day-${entry.dayStart}"
                    is ListEntry.Item -> entry.record.id
                }
            },
            contentType = { if (it is ListEntry.Item) 1 else 0 },
        ) { entry ->
            when (entry) {
                is ListEntry.DayHeader -> Text(
                    text = entry.label,
                    style = Ns.type.subhead,
                    fontWeight = FontWeight.SemiBold,
                    color = Ns.colors.secondary,
                    modifier = Modifier
                        .padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
                        .semantics { heading() },
                )

                is ListEntry.Item -> NotificationRow(
                    record = entry.record,
                    time = entry.time,
                    showApp = showApp,
                    position = entry.position,
                    onClick = { onOpen(entry.record) },
                )
            }
        }
    }
}
