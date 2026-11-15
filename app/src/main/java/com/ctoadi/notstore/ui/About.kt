package com.ctoadi.notstore.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.ui.theme.Ns
import kotlinx.coroutines.launch

const val CREATOR = "aditya-gupta-dev"
const val CREATOR_URL = "https://github.com/aditya-gupta-dev"
const val REPO_URL = "https://github.com/aditya-gupta-dev/Notstore"

private val PillShape = RoundedCornerShape(50)

/**
 * "About" pill. When pressed, a band of light sweeps across it once, then the About dialog
 * opens. With animations turned off in system settings, the dialog opens straight away.
 */
@Composable
fun AboutButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ns.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sweep = remember { Animatable(0f) }
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
    val shine = c.accent.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(c.card)
            .drawWithContent {
                drawContent()
                val p = sweep.value
                if (p > 0f && p < 1f) {
                    val band = size.width * 0.5f
                    val x = -band + p * (size.width + band)
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, shine, Color.Transparent),
                            start = Offset(x, 0f),
                            end = Offset(x + band, size.height),
                        )
                    )
                }
            }
            .clickable(role = Role.Button, onClickLabel = "Show app information") {
                if (sweep.isRunning) return@clickable
                scope.launch {
                    if (!reduceMotion) {
                        sweep.snapTo(0f)
                        sweep.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
                    }
                    sweep.snapTo(0f)
                    onOpen()
                }
            }
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("About Notstore", style = Ns.type.headline, color = c.accent)
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit, onOpenLink: (String) -> Unit) {
    val c = Ns.colors
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        icon = { AppIcon(context.packageName, "Notstore", 56.dp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Notstore", style = Ns.type.title, color = c.ink)
                Text(version, style = Ns.type.subhead, color = c.secondary, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bg)
            ) {
                LinkLine("Made by", CREATOR) { onOpenLink(CREATOR_URL) }
                Hairline(Modifier.padding(start = 14.dp))
                LinkLine("Source code", "$CREATOR/Notstore") { onOpenLink(REPO_URL) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", style = Ns.type.headline, color = c.accent) }
        },
    )
}

@Composable
private fun LinkLine(label: String, value: String, onClick: () -> Unit) {
    val c = Ns.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open in browser", onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Ns.type.caption, color = c.secondary)
            Spacer(Modifier.height(2.dp))
            Text(value, style = Ns.type.body, color = c.accent)
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = c.secondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
