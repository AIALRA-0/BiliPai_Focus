package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt

/** 横屏侧面板按窗口边界预留空间，视频和控制栏读取同一份边界。 */
internal class PlayerPanelViewportState {
    private class Reservation(val bounds: IntRect, val dragDistancePx: Int, val closeProgressProvider: () -> Float)
    private val panels = mutableStateMapOf<Any, Reservation>()

    fun reserve(owner: Any, bounds: IntRect, dragDistancePx: Int = bounds.width, closeProgressProvider: () -> Float = { 0f }) {
        panels[owner] = Reservation(bounds, dragDistancePx, closeProgressProvider)
    }

    fun release(owner: Any) {
        panels.remove(owner)
    }

    fun endInsetPx(playerRight: Int): Int = panels.values.maxOfOrNull {
        resolvePlayerPanelVisibleEndInsetPx(playerRight, it.bounds, it.closeProgressProvider(), it.dragDistancePx)
    } ?: 0
}

internal fun resolvePlayerPanelVisibleEndInsetPx(
    playerRight: Int,
    bounds: IntRect,
    closeProgress: Float,
    dragDistancePx: Int = bounds.width,
): Int {
    if (bounds.width <= 0 || bounds.height <= 0 || closeProgress >= 1f) return 0
    val panelLeft = bounds.left + (dragDistancePx.coerceAtLeast(0) * closeProgress.coerceIn(0f, 1f)).roundToInt()
    return (playerRight - panelLeft).coerceAtLeast(0)
}

internal fun resolvePlayerPanelEndInsetPx(playerRight: Int, panels: Collection<IntRect>): Int {
    val left = panels.filter { it.width > 0 && it.height > 0 }.minOfOrNull { it.left }
        ?: return 0
    return (playerRight - left).coerceAtLeast(0)
}

internal val LocalPlayerPanelViewport = staticCompositionLocalOf<PlayerPanelViewportState?> { null }

@Composable
internal fun ReservePlayerPanelViewport(
    bounds: IntRect,
    enabled: Boolean,
    dragDistancePx: Int = bounds.width,
    closeProgressProvider: () -> Float = { 0f },
) {
    val viewport = LocalPlayerPanelViewport.current
    val owner = remember { Any() }
    val latestProgressProvider = androidx.compose.runtime.rememberUpdatedState(closeProgressProvider)
    DisposableEffect(viewport, owner, bounds, enabled, dragDistancePx) {
        if (enabled) viewport?.reserve(owner, bounds, dragDistancePx) { latestProgressProvider.value() }
        onDispose { viewport?.release(owner) }
    }
}
