package com.ctoadi.notstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Color roles. Each color has one job:
 *  - [accent] marks things you can tap and the data in charts
 *  - [signal] marks something that needs attention (always paired with text)
 *  - [danger] marks destructive actions
 *
 * Contrast against its usual surface is noted next to each value (WCAG ratio).
 */
@Immutable
data class NsColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val secondary: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val track: Color,
    val signal: Color,
    val signalSoft: Color,
    val danger: Color,
)

private val Light = NsColors(
    bg = Color(0xFFF1F2F6),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF12141C),        // 18.4:1 on card
    secondary = Color(0xFF575B6B),  // 6.7:1 on card, 6.0:1 on bg
    hairline = Color(0xFFE1E3EA),
    accent = Color(0xFF3644D9),     // 7.0:1 on card
    onAccent = Color(0xFFFFFFFF),   // 7.0:1 on accent
    accentSoft = Color(0xFFE4E7FB), // accent on it: 5.7:1
    track = Color(0xFFD5D9F3),
    signal = Color(0xFF8F4200),     // 6.1:1 on signalSoft
    signalSoft = Color(0xFFFCEBD3),
    danger = Color(0xFFB3261E),     // 6.5:1 on card
)

private val Dark = NsColors(
    bg = Color(0xFF0B0C11),
    card = Color(0xFF171922),
    ink = Color(0xFFF1F2F6),        // 15.7:1 on card
    secondary = Color(0xFFA3A7B8),  // 7.3:1 on card
    hairline = Color(0xFF2A2D3A),
    accent = Color(0xFF97A3FF),     // 7.5:1 on card
    onAccent = Color(0xFF0B0C11),   // 8.4:1 on accent
    accentSoft = Color(0xFF262A4D), // accent on it: 5.9:1
    track = Color(0xFF30345C),
    signal = Color(0xFFF2B45A),     // 7.5:1 on signalSoft
    signalSoft = Color(0xFF3A2A12),
    danger = Color(0xFFFF8A80),     // 7.7:1 on card
)

/** Type scale. Sizes are in sp, so everything follows the system font-size setting. */
object NsType {
    val largeTitle = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
    val title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val headline = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal)
    val subhead = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

    /** Fixed-width digits so counts line up in columns and don't jitter as they change. */
    const val TABULAR = "tnum"
}

private val LocalNsColors = staticCompositionLocalOf { Light }

object Ns {
    val colors: NsColors
        @Composable @ReadOnlyComposable get() = LocalNsColors.current
    val type = NsType
}

@Composable
fun NotstoreTheme(content: @Composable () -> Unit) {
    // Follows the system appearance. There is deliberately no in-app light/dark switch.
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light

    // Map the tokens onto Material so stock components (switches, dialogs, sheets) match.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = c.accent,
        background = c.bg,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.bg,
        onSurfaceVariant = c.secondary,
        surfaceContainerLowest = c.card,
        surfaceContainerLow = c.card,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.card,
        surfaceContainerHighest = c.hairline,
        outline = c.secondary,
        outlineVariant = c.hairline,
        error = c.danger,
        inverseSurface = c.ink,
        inverseOnSurface = c.card,
        inversePrimary = if (dark) Light.accent else Dark.accent,
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalNsColors provides c,
            LocalContentColor provides c.ink,
            content = content,
        )
    }
}
