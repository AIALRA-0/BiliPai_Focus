// 文件路径: feature/video/ui/components/VideoAspectRatio.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.feature.anime4k.gl.Anime4KDisplayScaleMode
import kotlin.math.roundToInt
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
//  已改用 MaterialTheme.colorScheme.primary

/**
 * 视频比例枚举
 * `resizeMode` 保留给旧偏好映射，真实渲染使用 `playerResizeMode`。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
enum class VideoAspectRatio(
    val displayName: String,
    val resizeMode: Int,
    val targetAspectRatio: Float? = null,
    val playerResizeMode: Int = resizeMode
) {
    FIT("适应", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("填充", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    /**
     * Fixed 16:9 frame: outer viewport is letterboxed to 16:9, content [FIT]s inside.
     * (Using ZOOM would crop non-16:9 sources and look like accidental FILL.)
     */
    RATIO_16_9(
        "16:9",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        targetAspectRatio = 16f / 9f,
        playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    ),
    /**
     * Fixed 4:3 frame: outer viewport is letterboxed to 4:3, content [FIT]s inside.
     */
    RATIO_4_3(
        "4:3",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        targetAspectRatio = 4f / 3f,
        playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    ),
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL);
    
    companion object {
        fun fromResizeMode(mode: Int): VideoAspectRatio {
            return entries.find { it.resizeMode == mode } ?: FIT
        }
    }
}

internal data class VideoViewportLayout(
    val width: Int,
    val height: Int
)

/**
 * BiliPai parity: never apply stretch/fill deformation to vertical content.
 * Cover (FILL/ZOOM) remains allowed for immersive portrait viewing.
 */
internal fun resolveSafeVideoAspectRatio(
    preferred: VideoAspectRatio,
    isVerticalVideo: Boolean
): VideoAspectRatio {
    if (!isVerticalVideo) return preferred
    return when (preferred) {
        VideoAspectRatio.STRETCH -> VideoAspectRatio.FIT
        else -> preferred
    }
}

internal fun resolveVideoViewportLayout(
    containerWidth: Int,
    containerHeight: Int,
    aspectRatio: VideoAspectRatio
): VideoViewportLayout {
    val safeWidth = containerWidth.coerceAtLeast(1)
    val safeHeight = containerHeight.coerceAtLeast(1)
    val targetAspectRatio = aspectRatio.targetAspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: return VideoViewportLayout(width = safeWidth, height = safeHeight)

    val containerAspectRatio = safeWidth.toFloat() / safeHeight.toFloat()
    return if (containerAspectRatio > targetAspectRatio) {
        VideoViewportLayout(
            width = (safeHeight * targetAspectRatio).roundToInt().coerceIn(1, safeWidth),
            height = safeHeight
        )
    } else {
        VideoViewportLayout(
            width = safeWidth,
            height = (safeWidth / targetAspectRatio).roundToInt().coerceIn(1, safeHeight)
        )
    }
}

/**
 * FIT / FILL / STRETCH 占满容器；固定 16:9 / 4:3 才需要 letterbox 的精确尺寸。
 * FILL 走 fillMaxSize 可避免上滑全屏首帧约束未稳定时用旧 px 钉死视口。
 */
internal fun shouldUseFillMaxPlayerViewport(aspectRatio: VideoAspectRatio): Boolean {
    return aspectRatio.targetAspectRatio == null
}

/**
 * Media3 [AspectRatioFrameLayout] 仅在 resizeMode 变化时 requestLayout。
 * 上滑全屏时容器尺寸变了但 mode 可能已是目标值（或首帧用了错误 measure），
 * 需要强制再测一次，否则 FILL/ZOOM 会留下右/下黑边。
 *
 * @return 实际应写入的 resizeMode（便于测试断言强制刷新策略）
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun resolveForcedPlayerResizeModePivot(targetResizeMode: Int): Int {
    return if (targetResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
}

/**
 * 将 [resizeMode] 应用到 PlayerView，并在需要时通过 mode 切换强制 remeasure。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun applyPlayerViewResizeMode(
    playerView: androidx.media3.ui.PlayerView,
    resizeMode: Int,
    forceRelayout: Boolean,
) {
    val current = playerView.resizeMode
    if (current != resizeMode) {
        playerView.resizeMode = resizeMode
    } else if (forceRelayout) {
        playerView.resizeMode = resolveForcedPlayerResizeModePivot(resizeMode)
        playerView.resizeMode = resizeMode
    }
    playerView.requestLayout()
    playerView.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_content_frame)
        ?.requestLayout()
    playerView.invalidate()
}

internal fun shouldRefreshMeasuredPlayerViewport(
    expectedWidth: Int,
    expectedHeight: Int,
    measuredWidth: Int,
    measuredHeight: Int
): Boolean {
    if (expectedWidth <= 0 || expectedHeight <= 0) return true
    return expectedWidth != measuredWidth || expectedHeight != measuredHeight
}

/**
 * 同步 + 下一帧再刷一次，覆盖上滑全屏首帧约束/ surface attach 竞态。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun schedulePlayerViewViewportRefresh(
    playerView: androidx.media3.ui.PlayerView,
    resizeMode: Int,
    expectedWidth: Int = 0,
    expectedHeight: Int = 0,
) {
    applyPlayerViewResizeMode(
        playerView = playerView,
        resizeMode = resizeMode,
        forceRelayout = true,
    )
    playerView.post {
        applyPlayerViewResizeMode(
            playerView = playerView,
            resizeMode = resizeMode,
            forceRelayout = true,
        )
        playerView.postOnAnimation {
            if (shouldRefreshMeasuredPlayerViewport(
                    expectedWidth = expectedWidth,
                    expectedHeight = expectedHeight,
                    measuredWidth = playerView.width,
                    measuredHeight = playerView.height
                )
            ) {
                applyPlayerViewResizeMode(
                    playerView = playerView,
                    resizeMode = resizeMode,
                    forceRelayout = true,
                )
            }
        }
    }
}

/**
 * 为直接绘制到 GL Surface 的视频计算内容视口。
 * PlayerView 会在内部处理 FIT/ZOOM，GL 输出必须显式保持源视频比例。
 */
internal fun VideoAspectRatio.toAnime4KDisplayScaleMode(): Anime4KDisplayScaleMode {
    return when (this) {
        VideoAspectRatio.FILL -> Anime4KDisplayScaleMode.CROP
        VideoAspectRatio.STRETCH -> Anime4KDisplayScaleMode.STRETCH
        VideoAspectRatio.FIT,
        VideoAspectRatio.RATIO_16_9,
        VideoAspectRatio.RATIO_4_3 -> Anime4KDisplayScaleMode.FIT
    }
}

/**
 * 视频比例选择菜单
 */
@Composable
fun AspectRatioMenu(
    currentRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerMiuixListPopup(
        title = "画面比例",
        onDismissRequest = onDismiss,
        modifier = modifier,
        placement = PlayerListPopupPlacement.END,
        minWidth = 160.dp,
    ) {
        VideoAspectRatio.entries.forEachIndexed { index, ratio ->
            val selected = ratio == currentRatio
            DropdownImpl(
                item = DropdownItem(text = ratio.displayName),
                optionSize = VideoAspectRatio.entries.size,
                isSelected = selected,
                index = index,
                enabled = !selected,
                onSelectedIndexChange = {
                    onRatioSelected(ratio)
                    onDismiss()
                },
            )
        }
    }
}

/**
 * 比例按钮（用于底部控制栏）
 */
@Composable
fun AspectRatioButton(
    currentRatio: VideoAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Chip),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            AppIcon(
                Icons.Outlined.Star,
                contentDescription = "画面比例",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            AppText(
                text = currentRatio.displayName,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
