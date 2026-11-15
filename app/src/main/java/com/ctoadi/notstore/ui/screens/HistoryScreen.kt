package com.ctoadi.notstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.HistoryState
import com.ctoadi.notstore.RangeChoice
import com.ctoadi.notstore.ui.RangeFilter
import com.ctoadi.notstore.ui.rangePhrase
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.EmptyState
import com.ctoadi.notstore.ui.NotificationList
import com.ctoadi.notstore.ui.ScreenPadding
import com.ctoadi.notstore.ui.ScreenTitle
import com.ctoadi.notstore.ui.theme.Ns

@Composable
fun HistoryScreen(
    state: HistoryState?,
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    range: RangeChoice,
    onRangeChange: (RangeChoice) -> Unit,
    onOpen: (NotificationRecord) -> Unit,
    onLoadMore: () -> Unit,
) {
    // The field owns its text so typing never waits on the search running in the background.
    var text by rememberSaveable { mutableStateOf(initialQuery) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = ScreenPadding)) {
            ScreenTitle("History")
            Spacer(Modifier.height(8.dp))
            SearchField(
                value = text,
                onValueChange = {
                    text = it
                    onQueryChange(it)
                },
            )
            Spacer(Modifier.height(8.dp))
            RangeFilter(range, onRangeChange)
        }
        when {
            state == null -> Unit

            state.items.isEmpty() && state.query.isBlank() &&
                range.preset != RangeChoice.Preset.All -> EmptyState(
                title = "No notifications${rangePhrase(range)}",
                body = "Choose a longer range to see more.",
            )

            state.items.isEmpty() && state.query.isBlank() -> EmptyState(
                title = "Nothing saved yet",
                body = "Notifications appear here as they arrive.",
            )

            state.items.isEmpty() -> EmptyState(
                title = "No results for “${state.query.trim()}”",
                body = "Check the spelling, or try a different word.",
            )

            else -> NotificationList(
                items = state.items,
                showApp = true,
                onOpen = onOpen,
                onNearEnd = onLoadMore,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val c = Ns.colors
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = Ns.type.body.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = c.secondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            "Search title, text or app",
                            style = Ns.type.body,
                            color = c.secondary,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = c.secondary,
                        )
                    }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
        },
    )
}
