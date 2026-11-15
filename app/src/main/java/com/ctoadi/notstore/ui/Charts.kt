package com.ctoadi.notstore.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.ctoadi.notstore.ui.theme.Ns

/**
 * A plain bar chart. Tap a bar to select it; tap it again to clear the selection.
 *
 * The chart draws no numbers itself. The caller shows a text summary and the selected bar's
 * value next to it, and passes [description] so screen readers get the same information.
 *
 * @param futureFrom bars from this index on haven't happened yet and are drawn as faint stubs.
 */
@Composable
fun BarChart(
    values: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    maxBarWidth: Dp = 30.dp,
    futureFrom: Int = values.size,
) {
    val c = Ns.colors
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(values.size) {
                detectTapGestures { offset ->
                    val index = (offset.x / (size.width.toFloat() / values.size))
                        .toInt().coerceIn(0, values.size - 1)
                    currentOnSelect(if (index == currentSelected) null else index)
                }
            }
    ) {
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = minOf(slot * 0.64f, maxBarWidth.toPx())
        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val stub = 3.dp.toPx()
        val minBar = 6.dp.toPx()
        val radius = CornerRadius(minOf(barWidth / 2, 5.dp.toPx()))

        values.forEachIndexed { i, v ->
            val barHeight = if (v == 0) stub else maxOf(minBar, size.height * v / max)
            val color = when {
                v == 0 || i >= futureFrom -> c.track
                selected != null && i != selected -> c.track
                else -> c.accent
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(slot * i + (slot - barWidth) / 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Weekday-by-hour heatmap. One hue, five steps from faint to strong, so darker means more.
 * Empty slots use the track color. Tap a cell to select it; tap again to clear.
 *
 * @param grid seven rows (Monday first) of 24 hourly counts.
 */
@Composable
fun Heatmap(
    grid: List<IntArray>,
    selected: Pair<Int, Int>?,
    onSelect: (Pair<Int, Int>?) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 18.dp,
    gap: Dp = 2.dp,
) {
    val c = Ns.colors
    val ramp = heatRamp(c.track, c.accent)
    val max = grid.maxOf { row -> row.maxOrNull() ?: 0 }.coerceAtLeast(1)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(cellHeight * 7 + gap * 6)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val row = (offset.y / (size.height / 7f)).toInt().coerceIn(0, 6)
                    val col = (offset.x / (size.width / 24f)).toInt().coerceIn(0, 23)
                    val cell = row to col
                    currentOnSelect(if (cell == currentSelected) null else cell)
                }
            }
    ) {
        val g = gap.toPx()
        val w = (size.width - g * 23) / 24
        val h = (size.height - g * 6) / 7
        val r = CornerRadius(minOf(w, h) * 0.22f)
        for (row in 0 until 7) for (col in 0 until 24) {
            val level = heatLevel(grid[row][col], max)
            val dim = selected != null && selected != (row to col)
            drawRoundRect(
                color = ramp[level].let { if (dim) it.copy(alpha = 0.35f) else it },
                topLeft = Offset(col * (w + g), row * (h + g)),
                size = Size(w, h),
                cornerRadius = r,
            )
        }
    }
}

/** 0 for none, then 1..4 in equal quarters of the busiest slot. */
fun heatLevel(value: Int, max: Int): Int =
    if (value <= 0) 0 else ((value * 4 + max - 1) / max).coerceIn(1, 4)

/** Five colors: the empty track, then four steps toward the full accent. */
fun heatRamp(track: Color, accent: Color): List<Color> =
    listOf(track) + listOf(0.3f, 0.52f, 0.76f, 1f).map { lerp(track, accent, it) }
