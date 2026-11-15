package com.ctoadi.notstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.ui.PrimaryButton
import com.ctoadi.notstore.ui.SystemActions
import com.ctoadi.notstore.ui.theme.Ns

/** First run: explains the one permission the app needs, then sends the user to grant it. */
@Composable
fun SetupScreen(actions: SystemActions) {
    val c = Ns.colors
    var showHelp by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(c.bg).safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            StackMark()
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Every notification, kept in one place",
                style = Ns.type.largeTitle,
                color = c.ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Notstore saves each notification as it arrives, even when the app is " +
                    "closed. Search them later, and see which apps interrupt you most.",
                style = Ns.type.body,
                color = c.ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Everything stays on this phone. Notstore has no internet permission, " +
                    "so it can't send your notifications anywhere.",
                style = Ns.type.body,
                color = c.secondary,
            )
            Spacer(Modifier.height(32.dp))
            PrimaryButton(
                text = "Allow notification access",
                onClick = actions.openNotificationAccess,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "In the list that opens, turn on Notstore.",
                style = Ns.type.subhead,
                color = c.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            TextButton(
                onClick = { showHelp = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Can't turn it on?", style = Ns.type.headline, color = c.accent)
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("If the switch is greyed out") },
            text = {
                Text(
                    "Android blocks this setting for apps installed outside the Play Store. " +
                        "Open Notstore's app info, tap the menu in the top corner, and choose " +
                        "\"Allow restricted settings\". Then come back and try again."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false; actions.openAppInfo() }) {
                    Text("Open app info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelp = false }) { Text("Close") }
            },
        )
    }
}

/** The app's mark: a stack of saved notifications, newest on top. */
@Composable
private fun StackMark() {
    val c = Ns.colors
    Column {
        listOf(1f to 1f, 1f to 0.6f, 0.62f to 0.32f).forEach { (width, alpha) ->
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .size(width = 72.dp * width, height = 14.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(c.accent.copy(alpha = alpha))
            )
        }
    }
}
