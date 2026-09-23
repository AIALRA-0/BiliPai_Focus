package com.android.purebilibili.core.ui.transition

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.pow
import kotlin.math.roundToInt

// 景深层（与 Hero 卡片放大配合，progress 0→1 同源）：
// 1) 页面绕被点击卡片中心轻微后退（无 bounds 时退回屏幕中心），系统状态栏保持原位
// 2) 飞卡由 overlay 独自承担放大；背景缩放只做远平面，避免双重缩放
// 3) blur：空间纵深（冻结层 + BlurEffect）。半径按 **dp** 定义、按密度换算
// 4) scrim 压暗：聚焦/可读
// - 冻结层：首帧 record 一次后只改 BlurEffect，禁止 live 重录
// - 压暗全程保留（含 HELD），避免打开完成后景深断裂
// - 返回：景深读取主驱动的已缓动进度，空间 landing pulse 不进入 effects
private const val VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_DP = 12f
private const val VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX = VideoHeroMotionTokens.OPEN_BLUR_QUANTUM_PX
/** 返回消糊段更粗量化，降低 BlurEffect 每帧更新次数。 */
internal const val VIDEO_CARD_TRANSITION_RETURN_BLUR_QUANTUM_PX = VideoHeroMotionTokens.RETURN_BLUR_QUANTUM_PX
// 保持全屏背景几何稳定（0% 缩放），杜绝状态栏脱节、四周边框黑边与底栏悬空；
// 空间纵深由动态高斯模糊、柔和暗化与飞卡自身物理升降投影承担。
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION = 0f
private const val VIDEO_CARD_TRANSITION_RELATED_SCALE_REDUCTION =
    VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION
private const val VIDEO_CARD_TRANSITION_PARTITION_SCALE_REDUCTION =
    VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION
// 保持遮罩克制，让页面后退与 shared 卡片放大承担主要层级对比。
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK = 0.14f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT = 0.08f
private const val VIDEO_CARD_TRANSITION_REDUCED_SCRIM_ALPHA = 0.08f
/** 景深缩放露出的边缘：至少压到这个 tint 强度，避免浅色主题读成「白条」。 */
private const val VIDEO_CARD_TRANSITION_SCALE_GAP_MIN_TINT_LIGHT = 0.36f
private const val VIDEO_CARD_TRANSITION_SCALE_GAP_MIN_TINT_DARK = 0.44f
private val VIDEO_CARD_TRANSITION_DARK_GAP_BASE = Color(0xFF121212)

/**
 * 退后页满深度圆角兜底（dp）。
 * 优先用设备物理圆角（API 31+ WindowInsets）；未上报 / OEM 返回 0 时用此值。
 * 取 max(设备, 兜底)：缩到 90% 后过小的物理圆角读成尖角卡片。
 */
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CORNER_FALLBACK_DP = 24f

// 开场与返回时长由共享元素速度设置提供；取消仍固定为短恢复动画。
// 与共享元素标准时长对齐，避免景深先清完、封面还在赶路。
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS = 420
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS = 160

internal enum class VideoCardTransitionBackgroundPhase {
    IDLE,
    OPENING,
    HELD,
    RETURNING
}

/** 来源页类型：详情相关推荐复用首页景深，内嵌分区保留独立预算。 */
internal enum class VideoCardTransitionBackgroundSource {
    Home,
    RelatedVideo,
    Partition,
}

internal fun resolveVideoCardTransitionBackgroundSource(
    sourceRoute: String?,
): VideoCardTransitionBackgroundSource {
    return when (normalizeVideoCardTransitionRoute(sourceRoute)) {
        "partition" -> VideoCardTransitionBackgroundSource.Partition
        else -> if (sourceRoute?.substringBefore("?")?.startsWith("video/") == true) {
            VideoCardTransitionBackgroundSource.RelatedVideo
        } else {
            VideoCardTransitionBackgroundSource.Home
        }
    }
}

internal fun resolveVideoCardTransitionBackgroundScaleReduction(
    source: VideoCardTransitionBackgroundSource,
): Float = when (source) {
    VideoCardTransitionBackgroundSource.Home ->
        VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION
    VideoCardTransitionBackgroundSource.RelatedVideo ->
        VIDEO_CARD_TRANSITION_RELATED_SCALE_REDUCTION
    VideoCardTransitionBackgroundSource.Partition ->
        VIDEO_CARD_TRANSITION_PARTITION_SCALE_REDUCTION
}

/** Whether the retained source snapshot should receive the progress-driven blur effect. */
@Suppress("UNUSED_PARAMETER")
internal fun shouldUseRealtimeVideoCardTransitionBackgroundBlur(
    source: VideoCardTransitionBackgroundSource,
    realtimeBlurEnabled: Boolean,
): Boolean = realtimeBlurEnabled

/**
 * 详情压详情时保留真实父 entry，不录制整页 Host 快照。
 * Android player Surface 不属于 Compose GraphicsLayer，录制结果会把播放器区域变黑；
 * 返回相关推荐时直接让 NavDisplay 的父详情参与预览，才能保留真实组件和画面。
 */
internal fun shouldUseHostOwnedVideoCardTransitionSnapshot(
    sourceRoute: String?,
): Boolean = sourceRoute
    ?.substringBefore('?')
    ?.startsWith("video/") != true

/**
 * 首页 feed 自己录冻结层，顶栏 overlay 才能留在快照之上做独立滑入/滑出。
 */
internal fun shouldHomeFeedOwnVideoCardTransitionSnapshot(
    sourceRoute: String?,
    hasSnapshotHandle: Boolean,
): Boolean {
    if (!hasSnapshotHandle) return false
    if (!shouldUseHostOwnedVideoCardTransitionSnapshot(sourceRoute)) return false
    return sourceRoute?.substringBefore("?") == "home"
}

/** 首页 feed 已接管快照时，路由壳只钉页、不再整页录顶栏。 */
internal fun shouldApplyVideoCardTransitionSnapshotOnRouteShell(
    entryRoute: String?,
    sourceRoute: String?,
    activeMainHostRoute: String?,
): Boolean {
    if (!shouldUseHostOwnedVideoCardTransitionSnapshot(sourceRoute)) return false
    if (
        !shouldApplyVideoCardTransitionBackgroundToRoute(
            entryRoute = entryRoute,
            sourceRoute = sourceRoute,
            activeMainHostRoute = activeMainHostRoute,
        )
    ) {
        return false
    }
    return !shouldHomeFeedOwnVideoCardTransitionSnapshot(
        sourceRoute = sourceRoute,
        hasSnapshotHandle = true,
    )
}

internal data class VideoCardTransitionBackgroundFrame(
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float,
    val useLightScrimTint: Boolean = false,
    /** 退后页面（冻结层）的圆角半径，随景深线性建立。 */
    val cornerRadiusPx: Float = 0f,
    /** 随景深建立的外围漫反射阴影高度（px），使缩放空隙呈现下沉实体落阴。 */
    val shadowElevationPx: Float = 0f,
)

internal data class VideoCardTransitionBackgroundState(
    val progressProvider: () -> Float = { 0f },
    val sourceRouteProvider: () -> String? = { null },
    val sourceKeyProvider: () -> String? = { null },
    val phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.IDLE
    },
    val exposureProvider: () -> VideoCardTransitionExposure = {
        VideoCardTransitionExposure.Idle
    },
    val sourceCornerDpProvider: () -> Int? = { null },
    /** 被点击卡片的宿主坐标，供景深缩放绕卡片中心后退。 */
    val sourceBoundsProvider: () -> Rect? = { null },
    val snapshotHandle: VideoCardTransitionSnapshotHandle? = null,
    val isReturnGestureInProgressProvider: () -> Boolean = { false },
    val isGestureRestoreInProgressProvider: () -> Boolean = { false },
    val isQuickReturnFromDetailProvider: () -> Boolean = { false },
    /**
     * 为 true 时完整源卡常驻下层，由上层 live 画面在落点窗口内淡出交接。
     * 默认值仅供未接入导航宿主的调用方兜底；导航宿主应显式选择返回策略。
     */
    val preferWholeCardReturnProvider: () -> Boolean = { true },
    val motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    val isLightBackgroundProvider: () -> Boolean = { false },
    val realtimeBlurEnabledProvider: () -> Boolean = { false },
)

internal val LocalVideoCardTransitionBackgroundState = compositionLocalOf {
    VideoCardTransitionBackgroundState()
}

/**
 * 整卡过渡的景深档位只服从系统“减少动态效果”。
 *
 * 用户显式开启实时模糊后，运行时性能守卫不得把一次掉帧记忆成 scrim-only；否则后续
 * OPENING / RETURNING 都会失去动态模糊。来源页仍只冻结录制一次，性能由该策略控制。
 */
internal fun resolveVideoCardTransitionMotionTier(
    reduceMotion: Boolean,
): MotionTier = if (reduceMotion) MotionTier.Reduced else MotionTier.Normal

internal fun resolveVideoCardTransitionScrimAlpha(
    progress: Float,
    isLightBackground: Boolean,
    motionTier: MotionTier,
): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val maxAlpha = when {
        motionTier == MotionTier.Reduced ->
            VIDEO_CARD_TRANSITION_REDUCED_SCRIM_ALPHA
        isLightBackground ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT
        else ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK
    }
    return maxAlpha * clamped
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveVideoCardTransitionContentScale(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier,
    isGestureRestoreInProgress: Boolean,
    scaleReduction: Float = VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION,
): Float {
    if (phase == VideoCardTransitionBackgroundPhase.IDLE || motionTier == MotionTier.Reduced) {
        return 1f
    }
    val depthProgress = resolveVideoCardTransitionDepthProgress(
        progress = progress,
        phase = phase,
    )
    return 1f - scaleReduction.coerceIn(0f, 0.08f) * depthProgress
}

/**
 * 背景作为整页远平面围绕屏幕中心退后，点击不同卡片时不改变后退方向。
 * 飞卡自己的 bounds morph 仍从被点击卡片出发；这里只控制背后的列表与 chrome。
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolveVideoCardTransitionBackgroundScalePivot(
    sourceBounds: Rect?,
    canvasWidth: Float,
    canvasHeight: Float,
): Offset = Offset(0.5f, 0f)

internal fun resolveVideoCardTransitionOverlayDepthPivot(
    sourceBounds: Rect?,
    canvasWidth: Float,
    canvasHeight: Float,
    overlayWidth: Float,
    overlayHeight: Float,
): Offset {
    val canvasPivot = resolveVideoCardTransitionBackgroundScalePivot(
        sourceBounds = sourceBounds,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
    )
    val width = overlayWidth.coerceAtLeast(1f)
    val height = overlayHeight.coerceAtLeast(1f)
    return Offset(
        x = (canvasPivot.x * canvasWidth.coerceAtLeast(0f)) / width,
        y = (canvasPivot.y * canvasHeight.coerceAtLeast(0f)) / height,
    )
}

internal fun Modifier.videoCardTransitionChromeReveal(
    revealProvider: () -> Float,
    slideDown: Boolean,
): Modifier = this
    .graphicsLayer {
        alpha = revealProvider().coerceIn(0f, 1f)
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val reveal = revealProvider().coerceIn(0f, 1f)
        layout(placeable.width, placeable.height) {
            val dy = ((1f - reveal) * placeable.height).roundToInt()
            placeable.place(0, if (slideDown) dy else -dy)
        }
    }

internal fun Modifier.videoCardTransitionOverlayDepthEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    motionTierProvider: () -> MotionTier,
    sourceBoundsProvider: () -> Rect?,
    scaleReductionProvider: () -> Float = { VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION },
    densityProvider: () -> Float,
): Modifier {
    // Reuse the effect while quantization keeps the radius unchanged. Keep this cache local
    // to the modifier, outside snapshot state, so layer updates do not trigger composition.
    var cachedBlurRadiusPx = Float.NaN
    var cachedBlurEffect: BlurEffect? = null
    return this.graphicsLayer {
        val progress = progressProvider()
        val phase = phaseProvider()
        val motionTier = motionTierProvider()
        // Keep the live chrome on the same visual frame contract as the retained source page.
        // In particular, phase-specific radius quantization must match on both layers or their
        // shared edge reads as a sharp blur step during predictive back.
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = progress,
            phase = phase,
            motionTier = motionTier,
            isGestureRestoreInProgress = false,
            density = densityProvider(),
            scaleReduction = scaleReductionProvider(),
        )
        // The caller supplies a full-viewport layer, so use its measured coordinates instead
        // of LocalConfiguration. This keeps the pivot identical under edge-to-edge insets.
        val canvasW = size.width
        val canvasH = size.height
        val pivot = resolveVideoCardTransitionOverlayDepthPivot(
            sourceBounds = sourceBoundsProvider(),
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            overlayWidth = size.width,
            overlayHeight = size.height,
        )
        scaleX = frame.contentScale
        scaleY = frame.contentScale
        transformOrigin = TransformOrigin(pivot.x, pivot.y)
        if (frame.blurRadiusPx != cachedBlurRadiusPx) {
            cachedBlurRadiusPx = frame.blurRadiusPx
            cachedBlurEffect = if (frame.blurRadiusPx > 0.01f) {
                BlurEffect(frame.blurRadiusPx, frame.blurRadiusPx, TileMode.Clamp)
            } else {
                null
            }
        }
        renderEffect = cachedBlurEffect
    }
}

internal fun resolveVideoCardTransitionBackgroundScalePivotOffset(
    sourceBounds: Rect?,
    canvasSize: Size,
): Offset {
    val pivot = resolveVideoCardTransitionBackgroundScalePivot(
        sourceBounds = sourceBounds,
        canvasWidth = canvasSize.width,
        canvasHeight = canvasSize.height,
    )
    return Offset(
        x = pivot.x * canvasSize.width.coerceAtLeast(0f),
        y = pivot.y * canvasSize.height.coerceAtLeast(0f),
    )
}

internal fun resolveVideoCardTransitionBackgroundFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier = MotionTier.Normal,
    isLightBackground: Boolean = false,
    isGestureRestoreInProgress: Boolean = false,
    sdkInt: Int = Build.VERSION.SDK_INT,
    /** 屏幕密度（px/dp）。模糊/圆角按 dp 标定，调用方须传 DrawScope density。 */
    density: Float = 1f,
    /** 设备物理屏圆角（px）；0 表示未知，走 24dp 兜底。 */
    deviceCornerRadiusPx: Float = 0f,
    scaleReduction: Float = VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION,
): VideoCardTransitionBackgroundFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val depthProgress = resolveVideoCardTransitionDepthProgress(
        progress = clamped,
        phase = phase,
    )
    val blurStrength = resolveVideoCardTransitionBlurStrength(depthProgress)
    val maxBlurRadiusPx = resolveVideoCardTransitionMaxBlurRadiusPx(motionTier, density)
    // 仅系统减弱动画(Reduced) / API<31 跳过 GPU 模糊；不按机型降级峰值。
    val rawBlurRadiusPx = if (
        phase != VideoCardTransitionBackgroundPhase.IDLE &&
        maxBlurRadiusPx > 0f &&
        sdkInt >= Build.VERSION_CODES.S
    ) {
        maxBlurRadiusPx * blurStrength
    } else {
        0f
    }

    val blurQuantumPx = resolveVideoCardTransitionBlurQuantumPx(
        motionTier = motionTier,
        phase = phase,
    )
    val cornerRadiusPx = if (
        scaleReduction > 0.001f &&
        phase != VideoCardTransitionBackgroundPhase.IDLE &&
        motionTier != MotionTier.Reduced
    ) {
        resolveVideoCardTransitionBackgroundCornerRadiusPx(
            depthProgress = depthProgress,
            motionTier = motionTier,
            density = density,
            deviceCornerRadiusPx = deviceCornerRadiusPx,
        )
    } else {
        0f
    }
    val shadowElevationPx = if (cornerRadiusPx > 0.01f) {
        (16f * density * depthProgress).coerceIn(0f, 24f * density)
    } else {
        0f
    }
    return VideoCardTransitionBackgroundFrame(
        blurRadiusPx = quantizeVideoCardTransitionBlurRadius(
            radiusPx = rawBlurRadiusPx,
            maxRadiusPx = maxBlurRadiusPx,
            quantumPx = blurQuantumPx,
        ),
        scrimAlpha = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING,
            VideoCardTransitionBackgroundPhase.HELD,
            VideoCardTransitionBackgroundPhase.RETURNING ->
                resolveVideoCardTransitionScrimAlpha(
                    progress = depthProgress,
                    isLightBackground = isLightBackground,
                    motionTier = motionTier,
                )
            VideoCardTransitionBackgroundPhase.IDLE -> 0f
        },
        contentScale = resolveVideoCardTransitionContentScale(
            progress = clamped,
            phase = phase,
            motionTier = motionTier,
            isGestureRestoreInProgress = isGestureRestoreInProgress,
            scaleReduction = scaleReduction,
        ),
        useLightScrimTint = isLightBackground,
        cornerRadiusPx = cornerRadiusPx,
        shadowElevationPx = shadowElevationPx,
    )
}

/**
 * 读设备物理屏圆角（px）。取四角最大值。
 * API < 31 或 insets 未就绪 / OEM 返回 0 → 0（由 fallback dp 兜底）。
 */
internal fun resolveDeviceDisplayCornerRadiusPx(
    rootWindowInsets: android.view.WindowInsets?,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Float {
    if (
        sdkInt < Build.VERSION_CODES.S ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        rootWindowInsets == null
    ) {
        return 0f
    }
    val positions = intArrayOf(
        android.view.RoundedCorner.POSITION_TOP_LEFT,
        android.view.RoundedCorner.POSITION_TOP_RIGHT,
        android.view.RoundedCorner.POSITION_BOTTOM_LEFT,
        android.view.RoundedCorner.POSITION_BOTTOM_RIGHT,
    )
    var maxRadius = 0
    for (position in positions) {
        val radius = rootWindowInsets.getRoundedCorner(position)?.radius ?: 0
        if (radius > maxRadius) maxRadius = radius
    }
    return maxRadius.toFloat()
}

/**
 * 满深度圆角（dp）：max(设备物理圆角换算 dp, [fallbackDp])。
 */
internal fun resolveVideoCardTransitionBackgroundCornerRadiusDp(
    deviceCornerRadiusPx: Float,
    density: Float,
    fallbackDp: Float = VIDEO_CARD_TRANSITION_BACKGROUND_CORNER_FALLBACK_DP,
): Float {
    val densitySafe = density.coerceAtLeast(0.01f)
    val deviceDp = if (deviceCornerRadiusPx > 0f) {
        deviceCornerRadiusPx / densitySafe
    } else {
        0f
    }
    return maxOf(deviceDp, fallbackDp.coerceAtLeast(0f))
}

/**
 * 退后页面圆角半径（px）：随景深线性建立。
 * 满深度 = max(设备圆角, 24dp 兜底) × density。
 * Reduced 档不缩放也不模糊，圆角同样跳过。
 */
internal fun resolveVideoCardTransitionBackgroundCornerRadiusPx(
    depthProgress: Float,
    motionTier: MotionTier,
    density: Float,
    deviceCornerRadiusPx: Float = 0f,
): Float {
    if (motionTier == MotionTier.Reduced) return 0f
    val fullRadiusDp = resolveVideoCardTransitionBackgroundCornerRadiusDp(
        deviceCornerRadiusPx = deviceCornerRadiusPx,
        density = density,
    )
    return fullRadiusDp *
        density.coerceAtLeast(0f) *
        depthProgress.coerceIn(0f, 1f)
}

/**
 * 预测式返回手势进行中时，把系统回退进度(0→1)映射为背景虚化进度(1→0)。
 *
 * - 手势起点(0)保持满虚化，与 [VideoCardTransitionBackgroundPhase.HELD] 无缝衔接；
 * - 拖到底(1)则背景基本清晰，从而让全屏 GPU 模糊随手势实时消退，
 *   与共享元素 morph 落位同步，避免提交返回后再补一段独立模糊。
 */
internal fun resolveVideoCardTransitionBackgroundGestureProgress(
    backProgress: Float
): Float {
    val clamped = backProgress.coerceIn(0f, 1f)
    return 1f - clamped
}

/**
 * [VideoCardTransitionBackgroundPhase.OPENING] 阶段预测式返回：以当前开场虚化进度为起点，
 * 随手势线性消退至清晰。与 HELD 满值起点的 [resolveVideoCardTransitionBackgroundGestureProgress] 区分。
 */
internal fun resolveVideoCardTransitionBackgroundOpeningGestureProgress(
    openingBlurProgress: Float,
    backProgress: Float,
): Float {
    val clampedOpening = openingBlurProgress.coerceIn(0f, 1f)
    val clampedBack = backProgress.coerceIn(0f, 1f)
    return clampedOpening * (1f - clampedBack)
}

internal fun isVideoCardTransitionBackgroundGesturePhase(
    phase: VideoCardTransitionBackgroundPhase,
): Boolean {
    return phase == VideoCardTransitionBackgroundPhase.HELD ||
        phase == VideoCardTransitionBackgroundPhase.OPENING
}

internal fun resolveVideoCardTransitionBackgroundGestureBlurProgress(
    phase: VideoCardTransitionBackgroundPhase,
    currentBlurProgress: Float,
    backProgress: Float,
): Float {
    return when (phase) {
        VideoCardTransitionBackgroundPhase.HELD ->
            resolveVideoCardTransitionBackgroundGestureProgress(backProgress)
        VideoCardTransitionBackgroundPhase.OPENING ->
            resolveVideoCardTransitionBackgroundOpeningGestureProgress(
                openingBlurProgress = currentBlurProgress,
                backProgress = backProgress,
            )
        else -> currentBlurProgress
    }
}

/**
 * 景深返回与共享元素使用同一个满进度时长；被打断时只按实际剩余进度缩短，
 * 不再切换到另一套“快速返回”节奏。
 */
internal fun resolveVideoCardTransitionReturnFullDurationMillis(
    baseDurationMillis: Int,
): Int {
    return baseDurationMillis.coerceAtLeast(0)
}

/**
 * 返回动画提交时，若手势已消解部分虚化(startProgress < 1)，剩余 [RETURNING] 动画按比例缩短。
 *
 * 默认 [minDurationMs] 仅用于**非 morph 对齐**的取消收尾；与 shell morph 同墙钟时请用
 * [resolveMorphAlignedDepthClearDurationMs]（min=0，禁止把糊拖过落位）。
 */
internal fun resolveVideoCardTransitionBackgroundReturnDurationMs(
    startProgress: Float,
    fullDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS,
    minDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
): Int {
    val clamped = startProgress.coerceIn(0f, 1f)
    val safeFull = fullDurationMs.coerceAtLeast(0)
    val raw = (safeFull * clamped).roundToInt()
    if (minDurationMs <= 0) return raw.coerceIn(0, safeFull.coerceAtLeast(0))
    val safeMin = minDurationMs.coerceAtMost(safeFull.coerceAtLeast(minDurationMs))
    return raw.coerceIn(safeMin, safeFull.coerceAtLeast(safeMin))
}

/**
 * 景深消糊时长与 shared morph 剩余时长锁步。
 *
 * - morphRemainingMs：shell bounds 后半段（或满程）墙钟
 * - blurStartProgress：提交时剩余模糊 1→0
 * - 结果 = morph * blur，Linear 播完时二者同时到 0，避免「卡已落位、背景还糊」
 */
internal fun resolveMorphAlignedDepthClearDurationMs(
    morphRemainingMs: Int,
    blurStartProgress: Float,
): Int {
    return resolveVideoCardTransitionBackgroundReturnDurationMs(
        startProgress = blurStartProgress,
        fullDurationMs = morphRemainingMs.coerceAtLeast(0),
        minDurationMs = 0,
    )
}

/**
 * OPENING 中途被返回打断时，必须从当前 progress 反转，禁止先补完进场再关。
 */
internal fun shouldInterruptVideoCardOpeningOnReturn(
    phase: VideoCardTransitionBackgroundPhase,
): Boolean = phase == VideoCardTransitionBackgroundPhase.OPENING

/**
 * 是否立刻掐掉景深模糊，避免封面落位后仍带 BlurEffect 闪一下。
 *
 * - 打断 [OPENING]：shared 常先落位，景深按比例消糊会拖尾 → 必 snap
 * - [HELD]/[RETURNING]（含快速返回）：**禁止** snap，必须从满糊连续落到清晰；
 *   否则背景会在封面落位瞬间“直接变清晰”，没有模糊→清晰过程
 *
 * [isQuickReturnFromDetail] 保留给调用方语义对齐；HELD/RETURNING 不再因快速返回 snap。
 */
@Suppress("UNUSED_PARAMETER")
internal fun shouldSnapClearVideoCardDepthBlurOnQuickReturn(
    isQuickReturnFromDetail: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
): Boolean = phase == VideoCardTransitionBackgroundPhase.OPENING

internal fun shouldApplyVideoCardTransitionBackgroundToRoute(
    entryRoute: String?,
    sourceRoute: String?,
    activeMainHostRoute: String?
): Boolean {
    val normalizedEntryRoute = normalizeVideoCardTransitionRoute(entryRoute) ?: return false
    val normalizedSourceRoute = normalizeVideoCardTransitionRoute(sourceRoute) ?: return false
    if (!isVideoCardReturnTargetRoute(normalizedSourceRoute)) return false
    if (normalizedEntryRoute == normalizedSourceRoute) return true
    val normalizedActiveMainHostRoute = normalizeVideoCardTransitionRoute(activeMainHostRoute)
    if (
        normalizedEntryRoute == "main_host" &&
        normalizedActiveMainHostRoute == normalizedSourceRoute
    ) {
        return true
    }
    // 首页顶栏内嵌分区：共享元素 source 是 partition，视觉宿主仍是 home / main_host(home)。
    if (normalizedSourceRoute == "partition") {
        if (normalizedEntryRoute == "home") return true
        if (normalizedEntryRoute == "main_host" && normalizedActiveMainHostRoute == "home") {
            return true
        }
    }
    return false
}

/**
 * 详情压详情的真实父页景深。
 *
 * 这里直接变换仍在 NavDisplay 中的父 entry，不把整页录进 GraphicsLayer。这样既保留
 * 模糊、收缩、圆角和 scrim，也不会把独立渲染的 Player Surface 固化成黑色快照。
 */
@Composable
internal fun Modifier.videoCardTransitionLiveBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    exposureProvider: () -> VideoCardTransitionExposure,
    isGestureRestoreInProgressProvider: () -> Boolean = { false },
    motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    isLightBackgroundProvider: () -> Boolean = { false },
    realtimeBlurEnabledProvider: () -> Boolean = { false },
    scaleReductionProvider: () -> Float = { VIDEO_CARD_TRANSITION_RELATED_SCALE_REDUCTION },
    sourceBoundsProvider: () -> Rect? = { null },
): Modifier {
    val view = LocalView.current
    val screenDensity = LocalDensity.current.density
    var deviceCornerRadiusPx by remember { mutableFloatStateOf(0f) }
    SideEffect {
        deviceCornerRadiusPx = resolveDeviceDisplayCornerRadiusPx(view.rootWindowInsets)
    }
    val frameState = remember(
        progressProvider,
        phaseProvider,
        exposureProvider,
        isGestureRestoreInProgressProvider,
        motionTierProvider,
        isLightBackgroundProvider,
        scaleReductionProvider,
        screenDensity,
    ) {
        derivedStateOf {
            val exposure = exposureProvider()
            val renderDecision = resolveVideoCardTransitionRenderDecision(exposure)
            val retainHeldDepth = shouldPaintRetainedSourceWithoutTransitionBackground(
                renderDecision
            )
            val activePhase = if (
                renderDecision.drawTransitionBackground || retainHeldDepth
            ) {
                phaseProvider()
            } else {
                VideoCardTransitionBackgroundPhase.IDLE
            }
            resolveVideoCardTransitionBackgroundFrame(
                progress = if (retainHeldDepth) 1f else progressProvider(),
                phase = activePhase,
                motionTier = motionTierProvider(),
                isLightBackground = isLightBackgroundProvider(),
                isGestureRestoreInProgress = isGestureRestoreInProgressProvider(),
                density = screenDensity,
                deviceCornerRadiusPx = deviceCornerRadiusPx,
                scaleReduction = scaleReductionProvider(),
            )
        }
    }

    return this
        .drawWithContent {
            val frame = frameState.value
            if (shouldDrawVideoCardTransitionScaleGapFill(frame.contentScale)) {
                drawRect(
                    resolveVideoCardTransitionScaleGapFillColor(
                        isLightBackground = frame.useLightScrimTint,
                        scrimAlpha = frame.scrimAlpha,
                    )
                )
            }
            drawContent()
            if (frame.scrimAlpha > 0.001f) {
                val scrimColor = if (frame.useLightScrimTint) {
                    VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                } else {
                    Color.Black
                }
                drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
            }
        }
        .graphicsLayer {
            val frame = frameState.value
            val pivot = resolveVideoCardTransitionBackgroundScalePivot(
                sourceBounds = sourceBoundsProvider(),
                canvasWidth = size.width,
                canvasHeight = size.height,
            )
            scaleX = frame.contentScale
            scaleY = frame.contentScale
            transformOrigin = TransformOrigin(pivot.x, pivot.y)
            val shouldClip = frame.cornerRadiusPx > 0.01f
            clip = shouldClip
            if (shouldClip) {
                shape = RoundedCornerShape(
                    (frame.cornerRadiusPx / screenDensity.coerceAtLeast(0.01f)).dp
                )
                shadowElevation = frame.shadowElevationPx
            } else {
                shadowElevation = 0f
            }
            renderEffect = if (
                realtimeBlurEnabledProvider() && frame.blurRadiusPx > 0.01f
            ) {
                BlurEffect(
                    radiusX = frame.blurRadiusPx,
                    radiusY = frame.blurRadiusPx,
                    edgeTreatment = TileMode.Clamp,
                )
            } else {
                null
            }
        }
}

/**
 * 视频卡片过渡期间 Nav 层全屏 backdrop：填补 sharedBounds morph / 预测式返回
 * 在屏幕边缘露出的窗口底色，视觉上延续首页虚化后的色调。
 */
internal data class VideoCardTransitionNavBackdropFrame(
    val scrimAlpha: Float,
    val useLightScrimTint: Boolean,
)

internal fun shouldShowVideoCardTransitionNavBackdrop(
    cardTransitionEnabled: Boolean,
    exposure: VideoCardTransitionExposure,
    isVideoDetailOnStack: Boolean,
    isReturningToVideoDetail: Boolean = false,
): Boolean {
    if (!cardTransitionEnabled || isReturningToVideoDetail) return false
    val decision = resolveVideoCardTransitionRenderDecision(exposure)
    // pop 提交后栈顶已是来源页，但共享壳仍在 overlay 中回收；背景必须留到 Returning 结束。
    if (exposure == VideoCardTransitionExposure.Returning) return decision.drawNavBackdrop
    if (!isVideoDetailOnStack) return false
    return decision.drawNavBackdrop
}

internal fun resolveVideoCardTransitionNavBackdropFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    isLightBackground: Boolean,
): VideoCardTransitionNavBackdropFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val scrimAlpha = when (phase) {
        VideoCardTransitionBackgroundPhase.OPENING,
        VideoCardTransitionBackgroundPhase.HELD,
        VideoCardTransitionBackgroundPhase.RETURNING ->
            resolveVideoCardTransitionScrimAlpha(
                progress = clamped,
                isLightBackground = isLightBackground,
                motionTier = MotionTier.Normal,
            )
        else -> 0f
    }
    return VideoCardTransitionNavBackdropFrame(
        scrimAlpha = scrimAlpha,
        useLightScrimTint = isLightBackground,
    )
}

internal fun resolveVideoCardTransitionNavBackdropColor(
    baseBackgroundColor: Color,
    frame: VideoCardTransitionNavBackdropFrame,
): Color {
    return resolveVideoCardTransitionScaleGapFillColor(
        isLightBackground = frame.useLightScrimTint,
        scrimAlpha = frame.scrimAlpha,
        baseBackgroundColor = baseBackgroundColor,
    )
}

/**
 * 景深 scale<1 时，缩放层四周会露出父级/窗口底色。
 * 用与 blur scrim 同向的不透明填充盖住空隙，避免预测性返回读成右侧白条。
 */
internal fun shouldDrawVideoCardTransitionScaleGapFill(contentScale: Float): Boolean {
    return contentScale < 0.999f
}

internal fun resolveVideoCardTransitionScaleGapFillColor(
    isLightBackground: Boolean,
    scrimAlpha: Float,
    baseBackgroundColor: Color = if (isLightBackground) {
        Color.White
    } else {
        VIDEO_CARD_TRANSITION_DARK_GAP_BASE
    },
): Color {
    val tint = if (isLightBackground) {
        VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
    } else {
        Color.Black
    }
    val minTint = if (isLightBackground) {
        VIDEO_CARD_TRANSITION_SCALE_GAP_MIN_TINT_LIGHT
    } else {
        VIDEO_CARD_TRANSITION_SCALE_GAP_MIN_TINT_DARK
    }
    val fraction = maxOf(scrimAlpha, minTint).coerceIn(0f, 1f)
    return lerp(
        start = baseBackgroundColor,
        stop = tint,
        fraction = fraction,
    )
}

/**
 * 是否用「冻结 display list + 动态 blur/scale」路径。
 * Reduced / API<31 走轻量 scrim-only，避免无收益的 layer 开销。
 */
/**
 * Host 在 NavDisplay **下方**。预测/返回时源页会重新 compose 并盖住 Host，
 * 因此 [BackPreview]/[Returning]/[Restoring] **必须由源页自己画冻结层+模糊**。
 *
 * 仅 [SettledHidden] 时详情盖住源页：源可跳过同 layer 绘制，交给 Host 预热满糊。
 */
/**
 * 历史 API：曾让 SettledHidden 源页空画交给 Host。
 * 空画在源仍 compose 时会黑洞；Host 也常因 stale 不 paint → 整屏黑。
 * 现恒 false：源页始终走完整 draw 路径（live 或冻结层）。
 */
@Suppress("UNUSED_PARAMETER")
internal fun shouldSourceYieldDepthLayerToHost(
    isHostOwnedSnapshot: Boolean,
    exposure: VideoCardTransitionExposure,
): Boolean = false

internal fun shouldUseVideoCardTransitionSnapshotBlur(
    exposure: VideoCardTransitionExposure,
    motionTier: MotionTier,
    realtimeBlurEnabled: Boolean = false,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean {
    if (!resolveVideoCardTransitionRenderDecision(exposure).updateBlurEffect) return false
    if (motionTier == MotionTier.Reduced) return false
    if (!realtimeBlurEnabled) return false
    return sdkInt >= Build.VERSION_CODES.S
}

/**
 * 每帧内多次读取同一 frame 时，用 (progress, phase, …) 缓存避免重复纯函数计算。
 */
internal class VideoCardTransitionBackgroundFrameCache {
    private var lastProgress = Float.NaN
    private var lastPhase: VideoCardTransitionBackgroundPhase? = null
    private var lastMotionTier: MotionTier? = null
    private var lastIsLightBackground: Boolean? = null
    private var lastGestureRestoreInProgress: Boolean? = null
    private var lastDensity = Float.NaN
    private var lastDeviceCornerRadiusPx = Float.NaN
    private var lastScaleReduction = Float.NaN
    private var cached = VideoCardTransitionBackgroundFrame(
        blurRadiusPx = 0f,
        scrimAlpha = 0f,
        contentScale = 1f,
    )

    fun resolve(
        progress: Float,
        phase: VideoCardTransitionBackgroundPhase,
        motionTier: MotionTier,
        isLightBackground: Boolean,
        isGestureRestoreInProgress: Boolean,
        density: Float,
        deviceCornerRadiusPx: Float,
        scaleReduction: Float,
    ): VideoCardTransitionBackgroundFrame {
        if (
            progress != lastProgress ||
            phase != lastPhase ||
            motionTier != lastMotionTier ||
            isLightBackground != lastIsLightBackground ||
            isGestureRestoreInProgress != lastGestureRestoreInProgress ||
            density != lastDensity ||
            deviceCornerRadiusPx != lastDeviceCornerRadiusPx ||
            scaleReduction != lastScaleReduction
        ) {
            lastProgress = progress
            lastPhase = phase
            lastMotionTier = motionTier
            lastIsLightBackground = isLightBackground
            lastGestureRestoreInProgress = isGestureRestoreInProgress
            lastDensity = density
            lastDeviceCornerRadiusPx = deviceCornerRadiusPx
            lastScaleReduction = scaleReduction
            cached = resolveVideoCardTransitionBackgroundFrame(
                progress = progress,
                phase = phase,
                motionTier = motionTier,
                isLightBackground = isLightBackground,
                isGestureRestoreInProgress = isGestureRestoreInProgress,
                density = density,
                deviceCornerRadiusPx = deviceCornerRadiusPx,
                scaleReduction = scaleReduction,
            )
        }
        return cached
    }
}

/**
 * 冻结层状态：开场首帧 record 后停止重录 feed，只对静态 display list
 * 更新 scale / BlurEffect / scrim，实现「看起来实时的动态模糊」与稳帧共存。
 */
internal class VideoCardTransitionSnapshotLayerState {
    // Reused by both source and host renderers for the lifetime of the captured display list.
    private val blurEffects = LinkedHashMap<Float, BlurEffect>()
    fun blurEffect(radiusPx: Float, sdkInt: Int = Build.VERSION.SDK_INT): BlurEffect? {
        if (radiusPx <= 0.01f || !radiusPx.isFinite() || sdkInt < 31) return null
        return blurEffects.getOrPut(radiusPx) {
            if (blurEffects.size >= 256) blurEffects.remove(blurEffects.keys.first())
            BlurEffect(radiusX = radiusPx, radiusY = radiusPx, edgeTreatment = TileMode.Clamp)
        }
    }
    val frameCache = VideoCardTransitionBackgroundFrameCache()
    var freezeRecording: Boolean = false
    var hasRecordedContent: Boolean = false
    /**
     * Host 共享层在来源 Scene dispose 后 display list 往往已失效（黑/空），
     * 但 [hasRecordedContent] 仍为 true。返回/预测时必须重录真实首页，禁止再 draw 空层。
     */
    var displayListStale: Boolean = false
    /**
     * 源页 dispose 后置 true：下一次源页挂上 BackPreview/Returning 时强制重录真实首页。
     * 与 [displayListStale] 不同：预测返回可暂用 Host 的冻结层作为首帧景深，随后由来源页
     * 重录真实内容；普通 pop 仍等待来源页刷新以避免黑帧。
     */
    var needsSourceRefresh: Boolean = false
    var lastBlurRadiusPx: Float = Float.NaN
    var lastCornerRadiusPx: Float = Float.NaN

    fun invalidateRecordedContent() {
        blurEffects.clear()
        freezeRecording = false
        hasRecordedContent = false
        displayListStale = false
        needsSourceRefresh = false
        lastBlurRadiusPx = Float.NaN
        lastCornerRadiusPx = Float.NaN
    }

    fun markDisplayListStale() {
        // 保留 hasRecordedContent 语义给调试；绘制侧以 stale 为准强制重录。
        displayListStale = true
        freezeRecording = false
        lastBlurRadiusPx = Float.NaN
        lastCornerRadiusPx = Float.NaN
    }

    fun markDisplayListFresh() {
        hasRecordedContent = true
        displayListStale = false
        needsSourceRefresh = false
        freezeRecording = true
    }

    fun markSourceDetachedForRefresh() {
        // 下一次 BackPreview 先让 Host 维持冻结景深一个绘制帧，再由来源页重录并接手。
        needsSourceRefresh = true
        freezeRecording = false
        lastBlurRadiusPx = Float.NaN
    }

    fun reset() = invalidateRecordedContent()
}

/** 冻结层是否可安全 drawLayer（有内容且 display list 未过期）。 */
internal fun isVideoCardTransitionSnapshotDrawable(
    hasRecordedContent: Boolean,
    displayListStale: Boolean,
): Boolean = hasRecordedContent && !displayListStale

internal class VideoCardTransitionSnapshotHandle(
    val contentLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    val state: VideoCardTransitionSnapshotLayerState,
) {
    fun clearRenderEffect() {
        contentLayer.renderEffect = null
        contentLayer.shadowElevation = 0f
        state.lastBlurRadiusPx = Float.NaN
        state.lastCornerRadiusPx = Float.NaN
    }

    fun releaseSession() {
        clearRenderEffect()
        state.reset()
    }
}

/** 浅色 scrim 色，供 Host 景深层与源页 effect 共用。 */
internal val VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT = Color(0xFF8E8E93)

/**
 * 把 [frame] 写到冻结 [contentLayer]（scale / 圆角 / BlurEffect），不负责 draw。
 */
internal fun applyVideoCardTransitionSnapshotFrame(
    contentLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    snapshotState: VideoCardTransitionSnapshotLayerState,
    frame: VideoCardTransitionBackgroundFrame,
    canvasSize: androidx.compose.ui.geometry.Size,
    sourceBounds: Rect? = null,
) {
    // The route draw path may lower alpha during its final live-content handoff. This layer is
    // shared with the host-owned depth renderer, so every frame starts from an opaque baseline.
    contentLayer.alpha = 1f
    contentLayer.pivotOffset = resolveVideoCardTransitionBackgroundScalePivotOffset(
        sourceBounds = sourceBounds,
        canvasSize = canvasSize,
    )
    contentLayer.scaleX = frame.contentScale
    contentLayer.scaleY = frame.contentScale
    if (frame.cornerRadiusPx != snapshotState.lastCornerRadiusPx) {
        snapshotState.lastCornerRadiusPx = frame.cornerRadiusPx
        if (frame.cornerRadiusPx > 0.01f) {
            contentLayer.setRoundRectOutline(cornerRadius = frame.cornerRadiusPx)
            contentLayer.clip = true
            contentLayer.shadowElevation = frame.shadowElevationPx
        } else {
            contentLayer.setRectOutline()
            contentLayer.clip = false
            contentLayer.shadowElevation = 0f
        }
    } else if (frame.cornerRadiusPx > 0.01f) {
        contentLayer.shadowElevation = frame.shadowElevationPx
    }
    if (frame.blurRadiusPx != snapshotState.lastBlurRadiusPx) {
        snapshotState.lastBlurRadiusPx = frame.blurRadiusPx
        contentLayer.renderEffect = snapshotState.blurEffect(frame.blurRadiusPx)
        VideoCardTransitionDiagnostics.onBlurEffectUpdated()
    }
}

@Composable
internal fun rememberVideoCardTransitionSnapshotHandle(): VideoCardTransitionSnapshotHandle {
    val layer = rememberGraphicsLayer()
    return remember(layer) {
        VideoCardTransitionSnapshotHandle(
            contentLayer = layer,
            state = VideoCardTransitionSnapshotLayerState(),
        )
    }
}

/**
 * 是否对来源页做每帧 live 重录。
 *
 * 真机 gfxinfo：OPENING/RETURNING 全页 live 重录+模糊会把 p90/p99 拉到百毫秒级
 *（Slow UI thread / Slow issue draw commands）。默认关闭 live，改用冻结层 +
 * 进度驱动 BlurEffect——仍是动态模糊观感，成本可控。
 */
internal fun shouldLiveRecordVideoCardTransitionSnapshot(
    phase: VideoCardTransitionBackgroundPhase,
): Boolean {
    return false
}

/**
 * 卡片开合景深（来源路由上的录制 / 绘制端）：
 * - OPENING：首帧 record 进 Host [snapshotHandle]，BlurEffect 跟进度
 * - HELD / 预测 / 返回：Host 拥有冻结层生命周期；本 Modifier **dispose 不得 invalidate**
 *   Host 快照。源页若仍在 composition，可继续用同一层跟手改半径；否则由
 *   [VideoCardTransitionHostDepthLayer] 在 NavDisplay 下绘制。
 * - IDLE：Host 释放会话
 * - API 31 以下 / 实时模糊关闭：保留 scrim 与元素级缩放
 * - Reduced：只保留轻 scrim
 */
@Composable
internal fun Modifier.videoCardTransitionBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    exposureProvider: () -> VideoCardTransitionExposure,
    isGestureRestoreInProgressProvider: () -> Boolean = { false },
    motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    isLightBackgroundProvider: () -> Boolean = { false },
    realtimeBlurEnabledProvider: () -> Boolean = { false },
    scaleReductionProvider: () -> Float = { VIDEO_CARD_TRANSITION_BACKGROUND_SCALE_REDUCTION },
    sourceBoundsProvider: () -> Rect? = { null },
    snapshotHandle: VideoCardTransitionSnapshotHandle? = null,
): Modifier {
    val isHostOwnedSnapshot = snapshotHandle != null
    // A host-owned handle already supplies both resources; allocate the fallback only when
    // this source actually owns its snapshot (including after a handle ownership change).
    val effectiveSnapshotHandle = snapshotHandle ?: rememberVideoCardTransitionSnapshotHandle()
    val contentLayer = effectiveSnapshotHandle.contentLayer
    val snapshotState = effectiveSnapshotHandle.state
    val view = LocalView.current
    var deviceCornerRadiusPx by remember { mutableFloatStateOf(0f) }
    // Host 共享 handle：dispose 时不 wipe 会话，但标记 display list 过期。
    // 源页再次 compose（预测/返回）时强制重录真实首页，避免 draw 空层全黑。
    DisposableEffect(snapshotState, contentLayer, isHostOwnedSnapshot) {
        onDispose {
            if (shouldInvalidateSnapshotOnSourceDispose(isHostOwnedSnapshot = isHostOwnedSnapshot)) {
                contentLayer.renderEffect = null
                snapshotState.invalidateRecordedContent()
            } else {
                // Host 会话层：保留 OPENING 冻结帧供 SettledHidden 满糊预热。
                // 不标 displayListStale（否则完整进详情后 Host 无法预热 → 返回无糊）。
                // 仅标记 needsSourceRefresh，源页再次挂上时重录真实首页。
                contentLayer.renderEffect = null
                snapshotState.markSourceDetachedForRefresh()
            }
        }
    }
    // insets 首帧可能为空；每次重组刷新，开场前通常已就绪。
    SideEffect {
        deviceCornerRadiusPx = resolveDeviceDisplayCornerRadiusPx(view.rootWindowInsets)
    }
    val phase = phaseProvider()
    val exposure = exposureProvider()
    val motionTier = motionTierProvider()
    val renderDecision = resolveVideoCardTransitionRenderDecision(exposure)
    val useSnapshotBlur = shouldUseVideoCardTransitionSnapshotBlur(
        exposure = exposure,
        motionTier = motionTier,
        realtimeBlurEnabled = realtimeBlurEnabledProvider(),
    )
    val retainSnapshot = renderDecision.retainSourceSnapshot &&
        motionTier != MotionTier.Reduced &&
        realtimeBlurEnabledProvider() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    LaunchedEffect(phase, exposure, retainSnapshot, isHostOwnedSnapshot) {
        if (!retainSnapshot) {
            // Host 会话层由 Host IDLE 释放；源页不得因 SettledHidden 误 reset。
            if (!isHostOwnedSnapshot) {
                snapshotState.reset()
            }
            return@LaunchedEffect
        }
        when (exposure) {
            VideoCardTransitionExposure.Opening -> {
                // 新开一场：允许首帧 record；draw 侧只录一次后冻结。
                snapshotState.freezeRecording = false
                snapshotState.hasRecordedContent = false
                snapshotState.displayListStale = false
                withFrameNanos { }
                snapshotState.freezeRecording = true
            }
            VideoCardTransitionExposure.BackPreview,
            VideoCardTransitionExposure.Returning,
            VideoCardTransitionExposure.Restoring -> {
                // 源页重新挂上：stale 或 dispose 后的 needsSourceRefresh 都允许重录。
                val mustRefresh = snapshotState.needsSourceRefresh ||
                    !isVideoCardTransitionSnapshotDrawable(
                        hasRecordedContent = snapshotState.hasRecordedContent,
                        displayListStale = snapshotState.displayListStale,
                    )
                if (mustRefresh) {
                    snapshotState.freezeRecording = false
                    withFrameNanos { }
                } else {
                    snapshotState.freezeRecording = true
                }
            }
            VideoCardTransitionExposure.SettledHidden -> {
                snapshotState.freezeRecording = true
            }
            VideoCardTransitionExposure.Idle -> {
                if (!isHostOwnedSnapshot) {
                    snapshotState.reset()
                }
            }
        }
    }

    SideEffect {
        // Host 拥有层时 SettledHidden 不在源页 clear（源页通常已 dispose）。
        if (!renderDecision.updateBlurEffect) {
            if (isHostOwnedSnapshot &&
                exposure == VideoCardTransitionExposure.SettledHidden
            ) {
                return@SideEffect
            }
            if (!isHostOwnedSnapshot) {
                contentLayer.renderEffect = null
                snapshotState.lastBlurRadiusPx = Float.NaN
            }
        }
    }

    val liveRecordingActive = useSnapshotBlur &&
        shouldLiveRecordVideoCardTransitionSnapshot(
            phase = phase,
        )
    VideoCardTransitionLiveBlurHitchLogger(
        phaseProvider = phaseProvider,
        liveRecordingActive = liveRecordingActive,
    )

    return this.drawWithContent {
        val activePhase = phaseProvider()
        val activeExposure = exposureProvider()
        val activeDecision = resolveVideoCardTransitionRenderDecision(activeExposure)
        // SettledHidden：详情盖住时源页通常已 dispose；若仍 compose，画 live 防黑洞，
        // 不要 return 空画。Host 在 drawable 时另画冻结层。
        // BackPreview/Returning 绝不能 yield 空画。
        if (!activeDecision.drawTransitionBackground) {
            if (activeDecision.drawSourceNormally) {
                drawContent()
            } else if (shouldPaintRetainedSourceWithoutTransitionBackground(activeDecision)) {
                // SettledHidden：有可用快照则按满糊画；否则 live 防黑。
                if (
                    isVideoCardTransitionSnapshotDrawable(
                        hasRecordedContent = snapshotState.hasRecordedContent,
                        displayListStale = snapshotState.displayListStale,
                    )
                ) {
                    val heldFrame = snapshotState.frameCache.resolve(
                        progress = 1f,
                        phase = VideoCardTransitionBackgroundPhase.HELD,
                        motionTier = motionTierProvider(),
                        isLightBackground = isLightBackgroundProvider(),
                        isGestureRestoreInProgress = false,
                        density = density,
                        deviceCornerRadiusPx = deviceCornerRadiusPx,
                        scaleReduction = scaleReductionProvider(),
                    )
                    applyVideoCardTransitionSnapshotFrame(
                        contentLayer = contentLayer,
                        snapshotState = snapshotState,
                        frame = heldFrame,
                        canvasSize = size,
                        sourceBounds = sourceBoundsProvider(),
                    )
                    drawLayer(contentLayer)
                    VideoCardTransitionDiagnostics.onSourceLayerDrawn()
                } else {
                    drawContent()
                }
            }
            return@drawWithContent
        }
        val activeProgress = progressProvider()
        val activeMotionTier = motionTierProvider()
        val frame = snapshotState.frameCache.resolve(
            progress = activeProgress,
            phase = activePhase,
            motionTier = activeMotionTier,
            isLightBackground = isLightBackgroundProvider(),
            isGestureRestoreInProgress = isGestureRestoreInProgressProvider(),
            density = density,
            deviceCornerRadiusPx = deviceCornerRadiusPx,
            scaleReduction = scaleReductionProvider(),
        )
        val snapshotBlurActive = shouldUseVideoCardTransitionSnapshotBlur(
            exposure = activeExposure,
            motionTier = activeMotionTier,
            realtimeBlurEnabled = realtimeBlurEnabledProvider(),
        )
        if (!snapshotBlurActive) {
            // IDLE / Reduced / 低版本：正常绘制页面；元素缩放由各 shared shell 自己承担。
            drawContent()
            if (frame.scrimAlpha > 0.001f) {
                val scrimColor = if (frame.useLightScrimTint) {
                    VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                } else {
                    Color.Black
                }
                drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
            }
            return@drawWithContent
        }

        // OPENING 首录；预测/返回 DL stale 时重录。
        // 防黑：失效层绝不 drawLayer。防无糊：成功 record 后只画带 BlurEffect 的冻结层，
        // 不要先 drawContent 再叠层——部分机型上 live 底会压过/闪掉景深。
        val needsRecord = snapshotState.needsSourceRefresh ||
            !isVideoCardTransitionSnapshotDrawable(
                hasRecordedContent = snapshotState.hasRecordedContent,
                displayListStale = snapshotState.displayListStale,
            )
        if (
            needsRecord &&
            activeExposure == VideoCardTransitionExposure.BackPreview &&
            !snapshotState.freezeRecording &&
            isVideoCardTransitionSnapshotDrawable(
                hasRecordedContent = snapshotState.hasRecordedContent,
                displayListStale = snapshotState.displayListStale,
            )
        ) {
            // 来源页重挂的第一个预测帧不盖住 Host：Host 先画上一场的满模糊冻结层，
            // 下一帧再录制真实来源页并按 live back progress 消糊。
            snapshotState.freezeRecording = true
            return@drawWithContent
        }
        if (needsRecord) {
            if (size.width <= 0f || size.height <= 0f) {
                drawContent()
                if (frame.scrimAlpha > 0.001f) {
                    val scrimColor = if (frame.useLightScrimTint) {
                        VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                    } else {
                        Color.Black
                    }
                    drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
                }
                return@drawWithContent
            }
            contentLayer.record {
                this@drawWithContent.drawContent()
            }
            snapshotState.markDisplayListFresh()
            // 强制下一帧路径重绑 BlurEffect（dispose 时 renderEffect 已清）。
            snapshotState.lastBlurRadiusPx = Float.NaN
            VideoCardTransitionDiagnostics.onSnapshotRecorded()
        }

        val canDrawFrozenLayer = isVideoCardTransitionSnapshotDrawable(
            hasRecordedContent = snapshotState.hasRecordedContent,
            displayListStale = snapshotState.displayListStale,
        )
        if (!canDrawFrozenLayer) {
            // 唯一允许的清晰 live：冻结层不可用时（防黑）。
            drawContent()
            if (frame.scrimAlpha > 0.001f) {
                val scrimColor = if (frame.useLightScrimTint) {
                    VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                } else {
                    Color.Black
                }
                drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
            }
            return@drawWithContent
        }

        // 返回末段逐渐把画面交还给 live content。GraphicsLayer 冻结快照无法可靠
        // 捕获 PlayerView/TextureView 的实时像素，若它一直不透明地盖到 progress=0，
        // 上一级页面会在手势即将完成时仍发黑、发糊。交接区间与宽卡片的源内容
        // crossfade 保持一致，使实时页面与返回卡片同步显现。
        val frozenLayerAlpha = resolveVideoCardTransitionFrozenLayerAlpha(
            exposure = activeExposure,
            depthProgress = activeProgress,
        )
        val liveHandoffActive = frozenLayerAlpha < 0.999f

        // 返回末段 / 预览中段：先画 live content 让 hazeSource 重新登记，
        // 否则顶栏/底栏 unifiedBlur 在预测返回后会一直空白，直到再次进详情 remount。
        val shouldPrimeHazeSources = shouldPrimeLiveContentForHazeDuringDepthDraw(
            exposure = activeExposure,
            depthProgress = activeProgress,
        )
        if (shouldPrimeHazeSources || liveHandoffActive) {
            drawContent()
        }

        applyVideoCardTransitionSnapshotFrame(
            contentLayer = contentLayer,
            snapshotState = snapshotState,
            frame = frame,
            canvasSize = size,
            sourceBounds = sourceBoundsProvider(),
        )
        if (!liveHandoffActive && shouldDrawVideoCardTransitionScaleGapFill(frame.contentScale)) {
            drawRect(
                resolveVideoCardTransitionScaleGapFillColor(
                    isLightBackground = frame.useLightScrimTint,
                    scrimAlpha = frame.scrimAlpha,
                )
            )
        }
        contentLayer.alpha = frozenLayerAlpha
        if (frozenLayerAlpha > 0.001f) {
            drawLayer(contentLayer)
            VideoCardTransitionDiagnostics.onSourceLayerDrawn()
        }

        if (frame.scrimAlpha > 0.001f) {
            val scrimColor = if (frame.useLightScrimTint) {
                VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
            } else {
                Color.Black
            }
            drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
        }
    }
}

/**
 * Fades the frozen depth snapshot out over the full source-card takeover window.
 * Depth is 1 at full blur and 0 when the previous page is fully restored.
 *
 * Drawing live content underneath this layer is essential for player surfaces: recording an
 * Android view into a Compose [androidx.compose.ui.graphics.layer.GraphicsLayer] can yield a black
 * frame even though the actual surface is still rendering.
 */
internal fun resolveVideoCardTransitionFrozenLayerAlpha(
    exposure: VideoCardTransitionExposure,
    depthProgress: Float,
): Float {
    val supportsLiveHandoff = when (exposure) {
        VideoCardTransitionExposure.BackPreview,
        VideoCardTransitionExposure.Returning,
        VideoCardTransitionExposure.Restoring,
        -> true
        else -> false
    }
    if (!supportsLiveHandoff) return 1f
    // 冻结层往往录到了进场时被隐藏标题的卡片。在返回中段就把它交给
    // live source page，否则下方已就绪的标题/UP/统计仍会被无正文快照盖住。
    return 1f - resolveVideoCardWholeSourceReturnAlpha(depthProgress)
}

/**
 * When depth is nearly clear during back preview / return / restore, also paint live
 * content so [hazeSource] areas re-register. Frozen-only draws leave chrome blur empty.
 *
 * Progress is depth (1 = full blur held, 0 = clear). Prime when depth ≤ threshold.
 */
internal fun shouldPrimeLiveContentForHazeDuringDepthDraw(
    exposure: VideoCardTransitionExposure,
    depthProgress: Float,
    clearThreshold: Float = 0.35f,
): Boolean {
    return when (exposure) {
        VideoCardTransitionExposure.BackPreview,
        VideoCardTransitionExposure.Returning,
        VideoCardTransitionExposure.Restoring ->
            depthProgress.coerceIn(0f, 1f) <= clearThreshold
        else -> false
    }
}

/**
 * 进场/持有/返回：景深 progress 一律线性同源。
 *
 * 返回不再做 soft-clear 二次映射——主驱动已应用曲线，再 remap 会让模糊层
 * 落后于壳落位。遗留 [softClearVideoCardTransitionDepth] 仅供测试/兼容读取。
 */
internal fun resolveVideoCardTransitionDepthProgress(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
): Float {
    return resolveVisualProgress(progress, phase, VideoSharedTransitionDirection.ENTER)
}

/**
 * 遗留 soft-clear 曲线（主路径 RETURNING 已改线性锁步 morph）。
 * depth = 1 - (1 - p)^1.2，p=0.5 时约 0.56。
 */
internal fun softClearVideoCardTransitionDepth(progress: Float): Float {
    val remaining = (1f - progress.coerceIn(0f, 1f))
    val easedRemaining = remaining.toDouble().pow(1.2).toFloat()
    return (1f - easedRemaining).coerceIn(0f, 1f)
}

private fun resolveVideoCardTransitionBlurStrength(progress: Float): Float {
    // 与景深进度同源：模糊与背景下沉同步建立/消退，避免“先糊后沉”的分层错位。
    return progress.coerceIn(0f, 1f)
}

/**
 * 开合景深峰值模糊半径（px）。
 * - Reduced（仅系统减弱动画）：0
 * - Normal / Enhanced：统一 **12dp × 密度**，**不按机型降级**。
 *   旧值固定 20px 在高密度手机上仅 ≈7dp（景深偏弱）、低密度平板上 ≈13dp（偏强），
 *   按 dp 标定后各密度观感一致。
 */
internal fun resolveVideoCardTransitionMaxBlurRadiusPx(
    motionTier: MotionTier,
    density: Float = 1f,
): Float {
    return when (motionTier) {
        MotionTier.Reduced -> 0f
        MotionTier.Normal,
        MotionTier.Enhanced ->
            VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_DP * density.coerceAtLeast(0f)
    }
}

/**
 * Blur 半径量化步长。
 * RETURNING 用更粗步长（默认 4px）减少 BlurEffect 更新次数，保一镜到底同时降 GPU 抖动；
 * OPENING/HELD 仍用细步长，避免开场虚化阶梯感。
 */
internal fun resolveVideoCardTransitionBlurQuantumPx(
    motionTier: MotionTier,
    phase: VideoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE,
): Float {
    @Suppress("UNUSED_PARAMETER")
    val ignored = motionTier
    return if (phase == VideoCardTransitionBackgroundPhase.RETURNING) {
        VIDEO_CARD_TRANSITION_RETURN_BLUR_QUANTUM_PX
    } else {
        VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX
    }
}

private fun quantizeVideoCardTransitionBlurRadius(
    radiusPx: Float,
    maxRadiusPx: Float,
    quantumPx: Float = VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX,
): Float {
    if (radiusPx <= 0f || maxRadiusPx <= 0f) return 0f
    // Preserve the full-strength endpoint at fractional display densities.
    if (radiusPx >= maxRadiusPx) return maxRadiusPx
    val step = quantumPx.coerceAtLeast(0.5f)
    return ((radiusPx / step).roundToInt() * step).coerceIn(0f, maxRadiusPx)
}

private fun normalizeVideoCardTransitionRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        "home"
    } else {
        normalized.substringBefore("?")
    }
}
