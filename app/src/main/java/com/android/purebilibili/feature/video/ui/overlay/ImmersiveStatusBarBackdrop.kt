package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.unifiedBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import kotlin.math.roundToInt

internal fun resolveVideoStatusBarAmbientHazeStyle(): HazeBlurStyle = HazeBlurStyle {
    backgroundColor(Color.Black)
    colorEffects(emptyList())
    blurRadius(24.dp)
    noiseFactor(0f)
    fallbackColorEffect(HazeColorEffect.tint(Color.Black))
}

/**
 * 播放器顶部为系统状态栏预留的背景条，保证系统状态图标在视频画面上清晰可见。
 *
 * [useAmbientHaze] 开启（「播放页沉浸状态栏」开关）时，实时采样播放画面做毛玻璃模糊，
 * 状态栏背景跟随视频画面变化；关闭时保持纯黑背景（默认），视觉统一且零采样开销。
 * 黑色同时作为首帧与采样失败的兜底。
 *
 * [videoBoundsInWindow] 提供播放器 surface 在窗口坐标中的实时矩形：模糊条把采样帧
 * 摆放到 surface 实际覆盖的位置上，播放器下滑缩小时模糊内容跟随几何变化；
 * 传 null 时退化为整帧 Crop + [contentAlignment] 的旧映射。
 */
@Composable
internal fun ImmersiveStatusBarBackdrop(
    ambientFrame: State<ImageBitmap?>?,
    height: Dp,
    useAmbientHaze: Boolean,
    modifier: Modifier = Modifier,
    videoBoundsInWindow: (() -> Rect?)? = null,
) {
    ImmersiveAmbientLetterboxBackdrop(
        ambientFrame = ambientFrame,
        height = height,
        useAmbientHaze = useAmbientHaze,
        contentAlignment = Alignment.TopCenter,
        videoBoundsInWindow = videoBoundsInWindow,
        modifier = modifier,
    )
}

/**
 * 竖屏详情横屏视频上下黑边区域的动态模糊条（与状态栏沉浸采样同源）。
 * 顶部/底部各放一条，[contentAlignment] 决定裁切采样的对齐边。
 * [videoBoundsInWindow] 非空时按 surface 实际位置做几何映射（见 [ImmersiveStatusBarBackdrop]）。
 */
@Composable
internal fun ImmersiveAmbientLetterboxBackdrop(
    ambientFrame: State<ImageBitmap?>?,
    height: Dp,
    useAmbientHaze: Boolean,
    contentAlignment: Alignment = Alignment.TopCenter,
    modifier: Modifier = Modifier,
    videoBoundsInWindow: (() -> Rect?)? = null,
) {
    if (height.value <= 0f) return
    val currentAmbientFrame = if (useAmbientHaze) ambientFrame?.value else null
    val hazeState = rememberRecoverableHazeState()
    val colorFaithfulHazeStyle = remember { resolveVideoStatusBarAmbientHazeStyle() }
    var stripBoundsInWindow by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onGloballyPositioned { stripBoundsInWindow = it.boundsInWindow() }
            // Haze renders through an offscreen effect. Keep that render target inside the
            // letterbox rectangle; otherwise some GPUs expose its tiled edge as a diagonal,
            // stair-stepped overlay on top of the adjacent SurfaceView video.
            .clipToBounds()
            .background(Color.Black),
    ) {
        if (currentAmbientFrame != null) {
            // 不 remember：条的位置逐帧变化（下滑缩小为连续动画），直接每次重组重算，
            // 由 onGloballyPositioned 写回 stripBoundsInWindow 驱动重组。
            val framePlacement = resolveAmbientFramePlacementInStrip(
                stripBounds = stripBoundsInWindow,
                videoBounds = videoBoundsInWindow?.invoke(),
            )
            when {
                // 几何映射：surface 与条重叠时（沉浸模式下视频顶到屏幕最上沿），
                // 把采样帧按 surface 的实际投影摆放，播放器放大缩小时跟随几何变化。
                framePlacement != null -> {
                    val density = LocalDensity.current
                    Image(
                        bitmap = currentAmbientFrame,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset { framePlacement.offset }
                            .size(with(density) { framePlacement.size.toDpSize() })
                            .hazeSourceCompat(hazeState),
                    )
                }
                else -> {
                    // 无重叠（常规布局：视频贴在状态栏条下方）或不知道 surface 位置：
                    // 沿用整帧 Crop + 顶对齐的切片映射，把画面顶边“延伸”进状态栏区域。
                    Image(
                        bitmap = currentAmbientFrame,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = contentAlignment,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSourceCompat(hazeState),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .unifiedBlur(
                        hazeState = hazeState,
                        // This ambient effect has its own user-facing switch. Treating it as
                        // HEADER lets the global header-blur preference disable the effect
                        // while the low-resolution haze source remains visible and pixelated.
                        surfaceType = BlurSurfaceType.GENERIC,
                        blurStyleOverride = colorFaithfulHazeStyle,
                    )
                    .background(Color.Black.copy(alpha = 0.34f)),
            )
        }
    }
}

/** 采样帧在本条内的摆放位置：surface 矩形映射到条的局部坐标。 */
private data class AmbientFramePlacement(
    val offset: IntOffset,
    val size: androidx.compose.ui.geometry.Size,
)

private fun resolveAmbientFramePlacementInStrip(
    stripBounds: Rect?,
    videoBounds: Rect?,
): AmbientFramePlacement? {
    if (stripBounds == null || videoBounds == null) return null
    if (videoBounds.width <= 0f || videoBounds.height <= 0f) return null
    // 无交集（播放器缩小/下移后不再覆盖状态栏区域）时不绘制，保持黑底。
    if (videoBounds.right <= stripBounds.left || videoBounds.left >= stripBounds.right ||
        videoBounds.bottom <= stripBounds.top || videoBounds.top >= stripBounds.bottom
    ) {
        return null
    }
    return AmbientFramePlacement(
        offset = IntOffset(
            (videoBounds.left - stripBounds.left).roundToInt(),
            (videoBounds.top - stripBounds.top).roundToInt(),
        ),
        size = androidx.compose.ui.geometry.Size(videoBounds.width, videoBounds.height),
    )
}

/**
 * 竖屏页横屏视频 letterbox 上下黑边高度（各半）。
 * fillContainer 或无效尺寸时返回 0。
 */
internal fun resolvePortraitLetterboxBarHeightPx(
    containerHeightPx: Int,
    viewportHeightPx: Int,
    fillContainer: Boolean,
): Int {
    if (fillContainer || containerHeightPx <= 0 || viewportHeightPx <= 0) return 0
    val leftover = (containerHeightPx - viewportHeightPx).coerceAtLeast(0)
    if (leftover <= 1) return 0
    return leftover / 2
}
