package com.android.purebilibili.core.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.blur.ChromeBackdropSource

/** A wallpaper-only recording. Never attach page/chrome content to this source. */
val LocalGlobalWallpaperBackdropSource = compositionLocalOf<ChromeBackdropSource?> { null }
val LocalGlobalWallpaperUri = compositionLocalOf { "" }

// LayoutCoordinates is mutable and keeps its identity while a retained page moves. Store
// its position by value as well so movement invalidates the wallpaper display list.
private data class WallpaperReplayPlacement(
    val coordinates: LayoutCoordinates,
    val windowPosition: Offset,
)

/** Replay in screen coordinates, so retained entries never reveal the previous page. */
@Composable
fun Modifier.sharedGlobalWallpaperBackground(
    fallbackColor: Color,
    fallbackColorProvider: (() -> Color)? = null,
): Modifier {
    // Existing constant-color callers retain their background modifier. Moving surfaces
    // can provide a color read in draw, without subscribing composition to their position.
    fun Modifier.fallbackBackground(opaque: Boolean): Modifier =
        if (fallbackColorProvider == null) {
            background(if (opaque) fallbackColor.copy(alpha = 1f) else fallbackColor)
        } else {
            drawBehind {
                val color = fallbackColorProvider()
                drawRect(if (opaque) color.copy(alpha = 1f) else color)
            }
        }
    val visible = LocalGlobalWallpaperBackdropVisible.current
    val source = LocalGlobalWallpaperBackdropSource.current
        ?.takeIf { visible && it.isReady }
        ?: return fallbackBackground(opaque = false)
    // Replay directly: drawBackdrop's effect modifier requires Android 13 even when effects
    // are empty. Drawing the recorded layer itself also supports older Android versions.
    val placement = remember(source) { mutableStateOf<WallpaperReplayPlacement?>(null) }
    return fallbackBackground(opaque = true)
        .onGloballyPositioned {
            placement.value = WallpaperReplayPlacement(it, it.positionInWindow())
        }
        .drawWithContent {
            clipRect {
                with(source.backdrop) {
                    drawBackdrop(
                        density = this@drawWithContent,
                        coordinates = placement.value?.coordinates,
                    )
                }
            }
            drawContent()
        }
}
