package com.ctoadi.notstore.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.StorageInfo
import com.ctoadi.notstore.ui.AppIcon
import com.ctoadi.notstore.ui.GroupRow
import com.ctoadi.notstore.ui.RowPosition
import com.ctoadi.notstore.ui.ScreenPadding
import com.ctoadi.notstore.ui.notificationCount
import com.ctoadi.notstore.ui.prefetchIcons
import com.ctoadi.notstore.ui.rowPosition
import com.ctoadi.notstore.ui.theme.Ns

/** Where Notstore's space goes: a total, what it's made of, and which apps take the most. */
@Composable
fun CacheScreen(
    info: StorageInfo?,
    onBack: () -> Unit,
    onClearTemporary: () -> Unit,
) {
    val c = Ns.colors
    val context = LocalContext.current
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)
    LaunchedEffect(info?.byApp) { prefetchIcons(context, info?.byApp.orEmpty().map { it.pkg }) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = ScreenPadding, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings", tint = c.ink)
            }
            Text("Cache", style = Ns.type.title, modifier = Modifier.semantics { heading() })
        }
        if (info == null) return@Column

        val parts = listOf(
            "Saved notifications" to info.databaseBytes,
            "Temporary files" to info.tempBytes,
            "Settings" to info.settingsBytes,
        )
        val largestPart = parts.maxOf { it.second }.coerceAtLeast(1)
        val largestApp = info.byApp.maxOfOrNull { it.bytes }?.coerceAtLeast(1) ?: 1

        LazyColumn(
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
        ) {
            item(key = "total") {
                Column(Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)) {
                    Text(size(info.cacheBytes), style = Ns.type.largeTitle)
                    Text(
                        "${notificationCount(info.notificationCount)} · " +
                            "app itself: ${size(info.appBytes)}",
                        style = Ns.type.subhead,
                        color = c.secondary,
                    )
                }
            }

            item(key = "parts-header") { Header("Breakdown") }
            itemsIndexed(parts, key = { _, p -> "part-${p.first}" }) { i, (label, bytes) ->
                val clearable = label == "Temporary files" && bytes > 0
                GroupRow(
                    position = rowPosition(i, parts.size),
                    onClick = if (clearable) onClearTemporary else null,
                ) {
                    SizeRow(
                        title = label,
                        trailing = size(bytes),
                        share = bytes / largestPart.toFloat(),
                        subtitle = if (clearable) "Tap to clear" else null,
                    )
                }
            }

            if (info.byApp.isNotEmpty()) {
                item(key = "apps-header") { Header("By app") }
                itemsIndexed(info.byApp, key = { _, a -> "app-${a.pkg}" }) { i, app ->
                    GroupRow(position = rowPosition(i, info.byApp.size), dividerInset = 64.dp) {
                        Row(
                            Modifier.padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.pkg, app.label, 36.dp)
                            SizeRow(
                                title = app.label,
                                subtitle = notificationCount(app.count),
                                trailing = "≈ " + size(app.bytes),
                                share = app.bytes / largestApp.toFloat(),
                                startPadding = 12.dp,
                            )
                        }
                    }
                }
                item(key = "apps-footer") {
                    Text(
                        "Estimated from each app's saved text.",
                        style = Ns.type.subhead,
                        color = c.secondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = Ns.type.subhead,
        fontWeight = FontWeight.SemiBold,
        color = Ns.colors.secondary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
    )
}

/** A label and size, with a bar whose length compares it to the largest item in its group. */
@Composable
private fun SizeRow(
    title: String,
    trailing: String,
    share: Float,
    subtitle: String? = null,
    startPadding: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val c = Ns.colors
    Column(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .padding(start = startPadding, end = 16.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = Ns.type.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(trailing, style = Ns.type.body.copy(fontFeatureSettings = Ns.type.TABULAR), color = c.secondary)
        }
        if (subtitle != null) {
            Text(subtitle, style = Ns.type.subhead, color = c.secondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.track)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(share.coerceIn(0.01f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.accent)
            )
        }
    }
}
