package com.ctoadi.notstore.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import com.ctoadi.notstore.service.LiveIntents
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.theme.Ns

/** The full text of one notification, with the things you can do to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    record: NotificationRecord,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onDelete: (NotificationRecord) -> Unit,
) {
    val c = Ns.colors
    val context = LocalContext.current
    val canOpenApp = remember(record.pkg) {
        context.packageManager.getLaunchIntentForPackage(record.pkg) != null
    }
    // The notification's own tap action, if this app session still holds it.
    val tapAction = remember(record.key) { LiveIntents.get(record.key) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Open fully so the actions at the bottom are never hidden under the system bar.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
        contentColor = c.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(record.pkg, record.appLabel, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        record.appLabel,
                        style = Ns.type.headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        fullDateTime(context, record.postedAt),
                        style = Ns.type.subhead,
                        color = c.secondary,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SelectionContainer {
                Column {
                    if (record.title.isNotEmpty()) {
                        Text(record.title, style = Ns.type.title)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (record.body.isNotEmpty()) {
                        Text(record.body, style = Ns.type.body)
                    }
                }
            }
            if (record.link.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.bg)
                        .clickable(onClickLabel = "Open link") {
                            onDismiss()
                            onOpenLink(record.link)
                        }
                        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = c.secondary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        record.link,
                        style = Ns.type.subhead,
                        color = c.accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        copy(context, "Link", record.link)
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link", tint = c.secondary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (tapAction != null || canOpenApp) {
                PrimaryButton(
                    // "Open" fires the notification's own action, so it lands where the
                    // notification pointed. Without it, the app opens to its start screen.
                    text = if (tapAction != null) "Open" else "Open ${record.appLabel}",
                    onClick = {
                        onDismiss()
                        if (tapAction == null || !send(tapAction)) onOpenApp(record.pkg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    val text = listOf(record.title, record.body)
                        .filter { it.isNotEmpty() }.joinToString("\n")
                    copy(context, "Notification", text)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Copy text", style = Ns.type.headline, color = c.accent)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { onDelete(record) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
                Text("Delete from history", style = Ns.type.headline, color = c.danger)
            }
        }
    }
}

private fun copy(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Fires another app's notification action. Returns false if it no longer works. */
private fun send(intent: PendingIntent): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= 34) {
        // Android 14+ only lets the target open a screen if the sender (us, in the foreground)
        // explicitly allows it.
        val options = ActivityOptions.makeBasic()
        @Suppress("DEPRECATION")
        options.setPendingIntentBackgroundActivityStartMode(
            if (Build.VERSION.SDK_INT >= 36) ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
            else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        )
        intent.send(options.toBundle())
    } else {
        intent.send()
    }
}.isSuccess
