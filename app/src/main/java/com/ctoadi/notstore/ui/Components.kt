package com.ctoadi.notstore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ctoadi.notstore.data.NotificationRecord
import com.ctoadi.notstore.ui.theme.Ns
import com.ctoadi.notstore.ui.theme.NsType

val CardRadius = 20.dp
val ScreenPadding = 16.dp

@Composable
fun NsCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = Ns.colors.card,
        contentColor = Ns.colors.ink,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = Ns.type.largeTitle,
            color = Ns.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(text = subtitle, style = Ns.type.subhead, color = Ns.colors.secondary)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Ns.colors.accent,
            contentColor = Ns.colors.onAccent,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, style = Ns.type.headline, textAlign = TextAlign.Center)
    }
}

/** A card that says something needs the user's attention, and offers the fix. */
@Composable
fun Banner(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    attention: Boolean = true,
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
) {
    val c = Ns.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = if (attention) c.signalSoft else c.card,
        contentColor = c.ink,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (attention) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = if (attention) c.signal else c.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = Ns.type.headline)
            }
            Spacer(Modifier.height(4.dp))
            Text(body, style = Ns.type.subhead)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (dismissLabel != null) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissLabel, style = Ns.type.headline, color = c.secondary)
                    }
                }
                TextButton(onClick = onAction) {
                    Text(actionLabel, style = Ns.type.headline, color = c.accent)
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = Ns.type.title, color = Ns.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = Ns.type.body, color = Ns.colors.secondary, textAlign = TextAlign.Center)
    }
}

/** Where a row sits inside a rounded group, so only the group's outer corners are rounded. */
enum class RowPosition { Single, First, Middle, Last }

fun rowPosition(index: Int, count: Int): RowPosition = when {
    count == 1 -> RowPosition.Single
    index == 0 -> RowPosition.First
    index == count - 1 -> RowPosition.Last
    else -> RowPosition.Middle
}

private val SingleShape = RoundedCornerShape(CardRadius)
private val FirstShape = RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius)
private val LastShape = RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
private val TimeStyle = NsType.caption.copy(fontFeatureSettings = NsType.TABULAR)

fun RowPosition.shape(): Shape = when (this) {
    RowPosition.Single -> SingleShape
    RowPosition.First -> FirstShape
    RowPosition.Last -> LastShape
    RowPosition.Middle -> RectangleShape
}

val RowPosition.hasDivider: Boolean
    get() = this == RowPosition.First || this == RowPosition.Middle

/** A grouped-list row: rounded as a set, with a hairline between neighbours. */
@Composable
fun GroupRow(
    position: RowPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    dividerInset: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val shape = position.shape()
    Column(
        modifier
            .fillMaxWidth()
            .then(if (shape == RectangleShape) Modifier else Modifier.clip(shape))
            .background(Ns.colors.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
        if (position.hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = dividerInset),
                thickness = 1.dp,
                color = Ns.colors.hairline,
            )
        }
    }
}

@Composable
fun NotificationRow(
    record: NotificationRecord,
    time: String,
    showApp: Boolean,
    position: RowPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ns.colors
    GroupRow(
        position = position,
        modifier = modifier,
        onClick = onClick,
        dividerInset = if (showApp) 68.dp else 16.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (showApp) {
                AppIcon(record.pkg, record.appLabel, 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showApp) {
                        Text(
                            text = record.appLabel,
                            style = Ns.type.caption,
                            color = c.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = record.title.ifEmpty { record.body },
                            style = Ns.type.headline,
                            color = c.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(text = time, style = TimeStyle, color = c.secondary)
                }
                if (showApp && record.title.isNotEmpty()) {
                    Text(
                        text = record.title,
                        style = Ns.type.headline,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val showBody = record.body.isNotEmpty() && (showApp || record.title.isNotEmpty())
                if (showBody) {
                    Text(
                        text = record.body,
                        style = Ns.type.subhead,
                        color = c.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Ns.colors.hairline))
}
