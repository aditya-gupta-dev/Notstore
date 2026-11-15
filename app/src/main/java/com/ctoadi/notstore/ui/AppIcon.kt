package com.ctoadi.notstore.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ctoadi.notstore.ui.theme.Ns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private class IconResult(val bitmap: ImageBitmap?)

/** Each app's icon is decoded once, off the main thread, then shared by every row. */
private val iconCache = ConcurrentHashMap<String, IconResult>()
private const val ICON_PX = 144

private fun loadIcon(context: Context, pkg: String): IconResult =
    iconCache.getOrPut(pkg) {
        IconResult(
            runCatching {
                context.packageManager.getApplicationIcon(pkg)
                    .toBitmap(ICON_PX, ICON_PX).asImageBitmap()
            }.getOrNull()
        )
    }

/** Decodes icons ahead of time so rows already have them when they scroll into view. */
suspend fun prefetchIcons(context: Context, pkgs: List<String>) {
    val app = context.applicationContext
    withContext(Dispatchers.IO) {
        for (pkg in pkgs) if (iconCache[pkg] == null) loadIcon(app, pkg)
    }
}

/** The app's launcher icon, or its initial on a tinted tile if the app is no longer installed. */
@Composable
fun AppIcon(pkg: String, label: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val result by produceState(initialValue = iconCache[pkg], pkg) {
        if (value == null) value = withContext(Dispatchers.IO) { loadIcon(context, pkg) }
    }
    val shape = RoundedCornerShape(size * 0.26f)
    val bitmap = result?.bitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(shape).background(Ns.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            // Shown only once loading has finished without an icon, so rows don't flash a letter.
            if (result != null) {
                Text(
                    text = label.trim().take(1).uppercase(),
                    color = Ns.colors.accent,
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

