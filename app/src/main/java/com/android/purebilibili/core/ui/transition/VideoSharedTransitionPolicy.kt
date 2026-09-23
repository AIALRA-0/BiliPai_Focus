package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.adaptive.AdaptiveFoldPosture
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfileSpec
import com.android.purebilibili.core.ui.adaptive.toAdaptiveWidthClass
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt
import kotlin.math.hypot
import kotlin.math.abs

/** Internal tokens only: no preference key or user-selectable curve is introduced. */
internal object VideoHeroMotionTokens {
    // Programmatic back should settle a little faster than entry. 0.83 closely follows
    // mature container-transform timing (roughly 300ms return for a 360ms entry) while
    // keeping the shared bounds, player surface and depth effects on one fixed clock.
    const val RETURN_RATIO = 0.83f
    const val RETURN_MIN_MS = 220
    const val GEOMETRY_MIN_RATIO = 0.78f
    const val GEOMETRY_MAX_RATIO = 1.22f
    // Must stay 0: a flying-layer pulse undershoots the frozen card and the stationary
    // list card then pops in at full size (cover / info scale-jump on land).
    const val LANDING_COMPRESSION = 0f
    // Bound effect changes during entry while keeping finer steps than return.
    const val OPEN_BLUR_QUANTUM_PX = 2f
    const val RETURN_BLUR_QUANTUM_PX = 4f
    const val SPRING_DAMPING = 1f
    // Critical spring reaches the Nav driver's 0.0025 threshold at ~8.3 / omega seconds.
    const val SPRING_SETTLING_FACTOR = 8.3f
}

internal data class VideoHeroMotionSpec(
    val enterDurationMillis: Int,
    val returnDurationMillis: Int,
    val cancelDurationMillis: Int,
    val reducedMotion: Boolean = false,
) {
    val enterSpatialSpec: Easing get() = AppMotionEasing.Continuity
    // The driver itself must NOT overshoot: Miuix unloads an entry at relativeDepth <= -1.
    // Do not add a flying-layer landing pulse; it drifts off the frozen card bounds.
    val returnSpatialSpec: Easing get() = AppMotionEasing.Continuity
    val predictiveSeekSpec: Easing get() = LinearEasing
    val effectsEasing: Easing get() = LinearEasing // progress is already eased by the owner
    val handoffTimeline get() = VideoCardTransitionVisualTimeline
    val commitStiffness: Float get() = stiffness(returnDurationMillis)
    val cancelStiffness: Float get() = stiffness(cancelDurationMillis)

    private fun stiffness(duration: Int): Float {
        val omega = VideoHeroMotionTokens.SPRING_SETTLING_FACTOR * 1000f / duration.coerceAtLeast(1)
        return omega * omega
    }

    fun remainingDuration(from: Float, to: Float, cancel: Boolean = false): Int =
        ((if (cancel) cancelDurationMillis else returnDurationMillis) *
            abs(to.coerceIn(0f, 1f) - from.coerceIn(0f, 1f))).roundToInt()
}

/** Bounds are host-local px; normalize by density exactly once, before any animation starts. */
internal fun resolveVideoHeroMotionSpec(
    baseDurationMillis: Int,
    sourceBounds: Rect? = null,
    targetBounds: Rect? = null,
    density: Float = 1f,
    reducedMotion: Boolean = false,
): VideoHeroMotionSpec {
    if (baseDurationMillis <= 0) return VideoHeroMotionSpec(0, 0, 0, reducedMotion)
    if (reducedMotion) {
        val duration = VideoCardTransitionVisualTimeline.REDUCED_MOTION_DURATION_MILLIS
        return VideoHeroMotionSpec(duration, duration, duration, true)
    }
    val enter = (baseDurationMillis * resolveVideoSharedTransitionGeometryRatio(
        sourceBounds, targetBounds, density,
    )).roundToInt()
    val returning = (enter * VideoHeroMotionTokens.RETURN_RATIO).roundToInt()
        .coerceAtLeast(VideoHeroMotionTokens.RETURN_MIN_MS)
    return VideoHeroMotionSpec(enter, returning, returning)
}

internal fun resolveVideoSharedTransitionGeometryRatio(
    sourceBounds: Rect?,
    targetBounds: Rect?,
    density: Float = 1f,
): Float {
    fun Rect.usable() = listOf(left, top, right, bottom).all { it.isFinite() } &&
        width > 0f && height > 0f
    if (sourceBounds == null || targetBounds == null || !sourceBounds.usable() ||
        !targetBounds.usable() || !density.isFinite() || density <= 0f
    ) return 1f
    val travelDp = (hypot(targetBounds.center.x - sourceBounds.center.x,
        targetBounds.center.y - sourceBounds.center.y) +
        hypot(targetBounds.width - sourceBounds.width, targetBounds.height - sourceBounds.height)) / density
    val geometryDurationMs = 208f + travelDp.coerceIn(0f, 500f) / 3f
    return (geometryDurationMs / VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS)
        .coerceIn(VideoHeroMotionTokens.GEOMETRY_MIN_RATIO, VideoHeroMotionTokens.GEOMETRY_MAX_RATIO)
}

/** Identity at every depth. A non-1 value would undershoot the frozen card on land. */
internal fun resolveVideoHeroLandingScale(depth: Float, autoReturning: Boolean): Float {
    if (!autoReturning || VideoHeroMotionTokens.LANDING_COMPRESSION <= 0f) return 1f
    val u = ((1f - depth.coerceIn(0f, 1f) - 0.82f) / 0.18f).coerceIn(0f, 1f)
    val pulse = 16f * u * u * (1f - u) * (1f - u)
    return 1f - VideoHeroMotionTokens.LANDING_COMPRESSION * pulse
}

/** 设备自适应信息，用于视频卡片过渡差异化。 */
internal data class VideoTransitionAdaptiveInfo(
    val widthSizeClass: com.android.purebilibili.core.util.WindowWidthSizeClass =
        com.android.purebilibili.core.util.WindowWidthSizeClass.Compact,
    val foldPosture: AdaptiveFoldPosture = AdaptiveFoldPosture.None,
)

internal val LocalVideoTransitionAdaptiveInfo = compositionLocalOf {
    VideoTransitionAdaptiveInfo()
}

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal enum class VideoSharedTransitionPlaybackIntent {
    ImmediatePlayback,
    CoverFirst
}

internal fun resolveVideoSharedTransitionPlaybackIntent(
    clickToPlayEnabled: Boolean,
    forceImmediatePlayback: Boolean = false
): VideoSharedTransitionPlaybackIntent {
    return if (!forceImmediatePlayback && !clickToPlayEnabled) {
        VideoSharedTransitionPlaybackIntent.CoverFirst
    } else {
        VideoSharedTransitionPlaybackIntent.ImmediatePlayback
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldFadePlayerSurfaceOnDetailReturn(
    isLeaving: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent
): Boolean {
    // ImmediatePlayback 一镜到底：返回全程保留直播 surface，不为 handoff 封面淡出。
    // CoverFirst 本身无直播 surface 主导，也不走淡出。签名保留供测试/未来接线。
    return false
}

internal fun shouldUseDetailReturnCoverCrossfade(
    isLeaving: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent
): Boolean {
    // CoverFirst 返回仍叠封面防黑底；ImmediatePlayback 由直播画面填满 morph，不抢封面。
    return isLeaving && playbackIntent == VideoSharedTransitionPlaybackIntent.CoverFirst
}

internal enum class VideoSharedTransitionTargetMode {
    InlineCover,
    InlinePlayer,
    LandscapeFullscreen,
    PortraitFullscreen
}

/**
 * 共享元素 / Glass·Cinematic 等默认封面框比例。
 *
 * 列表卡三档见 [HomeFeedCardStyle]：16:9 / 4:3 / 16:10。
 * 此处默认 **16:10**；具体列表卡仍以 [resolveHomeFeedCardLayout] 传入的比例为准。
 */
internal const val VIDEO_SHARED_COVER_ASPECT_RATIO = 16f / 10f
private const val HOME_SOURCE_ROUTE = "home"
internal const val VIDEO_SHARED_TRANSITION_FAST_DURATION_MILLIS = 280
internal const val VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS = 360
internal const val VIDEO_SHARED_TRANSITION_SLOW_DURATION_MILLIS = 480
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS = 240
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS = 900
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS =
    VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS
private const val HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP =
    VideoCardTransitionVisualTimeline.SECONDARY_CONTENT_TRANSLATION_DP
private const val HOME_DETAIL_REVEAL_INITIAL_SCALE = 1f
private const val VIDEO_METADATA_SHARED_BOUNDS_RATIO = 1f
private const val VIDEO_METADATA_SHARED_BOUNDS_MIN_MILLIS = 200
private const val VIDEO_METADATA_SHARED_BOUNDS_MAX_MILLIS = 900
private const val HOME_SHARED_TRANSITION_CARD_CORNER_DP = 16
private const val HOME_SHARED_TRANSITION_PLAYER_CORNER_DP = 12
private const val DEFAULT_VIDEO_CARD_CORNER_DP = 12
private const val DEFAULT_VIDEO_PLAYER_CORNER_DP = 12
private const val DYNAMIC_VIDEO_CARD_CORNER_DP = 10
private const val WATCH_LATER_VIDEO_CARD_CORNER_DP = 8
// 兼容字段：bounds 进场/返回均走固定时长 tween（见 boundsTransform），不再驱动 spring  morph。
// spring 常量仅保留给遗留 settle buffer / 测试兼容读取。
private const val VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO = 0.86f
private const val VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO = 0.95f
private const val VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS = 240f
private const val VIDEO_CARD_HERO_SPRING_REFERENCE_DURATION_MILLIS = 360f
private const val VIDEO_CARD_HERO_SPRING_MIN_STIFFNESS = 50f
private const val VIDEO_CARD_HERO_SPRING_MAX_STIFFNESS = 500f
// 返回落位后的 suppression / 景深 IDLE 余量：盖过主时长，避免 overlay 卸层后状态抢跑。
// 返回已改回固定时长 tween，无需 spring 过冲额外时间，保留短 buffer 即可。
internal const val VIDEO_CARD_RETURN_SPRING_SETTLE_BUFFER_MS = 48
// 约 1px：遗留 spring API 收敛阈值。
private val VIDEO_CARD_HERO_BOUNDS_VISIBILITY_THRESHOLD = Rect(1f, 1f, 1f, 1f)
// 透明度与进场空间统一 Continuity，避免淡入淡出和位移抢拍。
private val VIDEO_CARD_ALPHA_EASING = AppMotionEasing.Continuity

enum class VideoSharedTransitionSpeed(val value: Int, val label: String) {
    FAST(0, "快速"),
    STANDARD(1, "标准"),
    SLOW(2, "慢速"),
    CUSTOM(3, "自定义");

    companion object {
        fun fromValue(value: Int): VideoSharedTransitionSpeed =
            entries.find { it.value == value } ?: STANDARD
    }
}

internal data class VideoSharedTransitionSpeedSettings(
    val speed: VideoSharedTransitionSpeed = VideoSharedTransitionSpeed.STANDARD,
    val customDurationMillis: Int = VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS
)

internal data class VideoSharedTransitionOwnership(
    val useCoverSharedBounds: Boolean,
    val useMetadataSharedBounds: Boolean,
    val useCardContainerSharedBounds: Boolean = false
)

internal data class VideoSharedTransitionMotionSpec(
    val enabled: Boolean,
    val durationMillis: Int,
    val fullscreenDurationMillis: Int,
    val contentDelayMillis: Int,
    val contentDurationMillis: Int,
    val contentSlideOffsetDp: Int,
    val contentInitialScale: Float,
    val enterSpatialDampingRatio: Float,
    val returnSpatialDampingRatio: Float,
    val spatialStiffness: Float,
    val enterAlphaEasing: Easing,
    val returnAlphaEasing: Easing
)

internal enum class VideoSharedTransitionDirection {
    ENTER,
    RETURN
}

internal data class VideoSharedCornerSpec(
    val enabled: Boolean,
    val startCornerDp: Int,
    val endCornerDp: Int
)

internal data class VideoSharedTransitionVisualSpec(
    val targetMode: VideoSharedTransitionTargetMode,
    val sourceCornerDp: Int,
    val targetCornerDp: Int,
    val fillTargetViewport: Boolean,
    val useCoverSharedBounds: Boolean,
    val suppressCoverFade: Boolean
)

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    return VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoCardSharedTransitionEnterEasing(): Easing =
    resolveVideoCardSharedTransitionSpatialEasing()

internal fun resolveVideoCardSharedTransitionReturnEasing(): Easing =
    resolveVideoCardSharedTransitionSpatialEasing()

/**
 * 卡片景深返回清晰曲线。
 *
 * **必须与 sharedBounds 返回 morph 一致用 Linear**：
 * morph 已是 Linear tween（可 seek / remainingDuration 可算），景深若用 SoftClear
 * 会在卡片将落位时仍「挂糊」，观感上模糊层和壳不同步。
 */
internal fun resolveVideoCardTransitionBackgroundReturnClearEasing(): Easing =
    LinearEasing

internal fun resolveVideoCardSharedTransitionSpatialEasing(): Easing = AppMotionEasing.Continuity

internal fun resolveVideoSharedTransitionSpatialStiffness(durationMillis: Int): Float {
    val safeDurationMillis = durationMillis.coerceAtLeast(1).toFloat()
    val durationRatio = VIDEO_CARD_HERO_SPRING_REFERENCE_DURATION_MILLIS / safeDurationMillis
    return (VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS * durationRatio * durationRatio)
        .coerceIn(
            VIDEO_CARD_HERO_SPRING_MIN_STIFFNESS,
            VIDEO_CARD_HERO_SPRING_MAX_STIFFNESS
        )
}

internal fun resolveVideoCardReturnSpringSettleBufferMs(): Long =
    VIDEO_CARD_RETURN_SPRING_SETTLE_BUFFER_MS.toLong()

internal fun resolveVideoSharedCoverCacheKey(
    videoIdentity: String,
    useLowQualityCover: Boolean = false,
): String {
    val qualityTag = if (useLowQualityCover) "s" else "n"
    return "cover_${videoIdentity.trim()}_${qualityTag}"
}

internal fun resolveVideoSharedTransitionDirection(
    initialBounds: Rect,
    targetBounds: Rect
): VideoSharedTransitionDirection {
    val initialArea = initialBounds.width * initialBounds.height
    val targetArea = targetBounds.width * targetBounds.height
    return if (targetArea < initialArea) {
        VideoSharedTransitionDirection.RETURN
    } else {
        VideoSharedTransitionDirection.ENTER
    }
}

internal fun resolveVideoSharedTransitionEasing(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect
): Easing {
    return when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> motion.enterAlphaEasing
        VideoSharedTransitionDirection.RETURN -> motion.returnAlphaEasing
    }
}

internal fun normalizeVideoSharedTransitionCustomDurationMillis(durationMillis: Int): Int {
    return durationMillis.coerceIn(
        VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS,
        VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS
    )
}

internal fun resolveVideoSharedTransitionDurationMillis(
    speedSettings: VideoSharedTransitionSpeedSettings,
    adaptiveInfo: VideoTransitionAdaptiveInfo = VideoTransitionAdaptiveInfo()
): Int {
    val baseMillis = when (speedSettings.speed) {
        VideoSharedTransitionSpeed.FAST -> VIDEO_SHARED_TRANSITION_FAST_DURATION_MILLIS
        VideoSharedTransitionSpeed.STANDARD -> VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS
        VideoSharedTransitionSpeed.SLOW -> VIDEO_SHARED_TRANSITION_SLOW_DURATION_MILLIS
        VideoSharedTransitionSpeed.CUSTOM ->
            normalizeVideoSharedTransitionCustomDurationMillis(speedSettings.customDurationMillis)
    }
    val widthFactor = when (adaptiveInfo.widthSizeClass) {
        com.android.purebilibili.core.util.WindowWidthSizeClass.Compact -> 1.0f
        com.android.purebilibili.core.util.WindowWidthSizeClass.Medium -> 1.1f
        com.android.purebilibili.core.util.WindowWidthSizeClass.Expanded -> 1.15f
        com.android.purebilibili.core.util.WindowWidthSizeClass.Large -> 1.25f
        com.android.purebilibili.core.util.WindowWidthSizeClass.ExtraLarge -> 1.3f
    }
    val deviceProfile = resolveDeviceUiProfileSpec(
        widthClass = adaptiveInfo.widthSizeClass.toAdaptiveWidthClass(),
        foldPosture = adaptiveInfo.foldPosture,
    )
    val postureFactor = if (deviceProfile.motionTier == MotionTier.Reduced) 0.85f else 1.0f
    return (baseMillis * widthFactor * postureFactor).roundToInt().coerceAtLeast(0)
}

internal fun resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis: Int): Int {
    return durationMillis
}

internal fun resolveVideoSharedTransitionContentDurationMillis(durationMillis: Int): Int {
    val normalizedDuration = VideoCardTransitionVisualTimeline.SECONDARY_CONTENT_ENTER_END -
        VideoCardTransitionVisualTimeline.SECONDARY_CONTENT_ENTER_START
    return (durationMillis.coerceAtLeast(0) * normalizedDuration).roundToInt()
}

internal fun resolveVideoSharedTransitionContentDelayMillis(durationMillis: Int): Int =
    (
        durationMillis.coerceAtLeast(0) *
            VideoCardTransitionVisualTimeline.SECONDARY_CONTENT_ENTER_START
        ).roundToInt()

internal fun resolveVideoSharedTransitionSourceCornerDp(
    sourceRoute: String?,
    fallbackCornerDp: Int = DEFAULT_VIDEO_CARD_CORNER_DP
): Int {
    return when (sourceRoute?.substringBefore("?")) {
        "dynamic",
        "dynamic_detail" -> DYNAMIC_VIDEO_CARD_CORNER_DP
        "watch_later" -> WATCH_LATER_VIDEO_CARD_CORNER_DP
        else -> fallbackCornerDp
    }.coerceAtLeast(0)
}

internal fun resolveVideoSharedTransitionVisualSpec(
    sourceRoute: String?,
    sourceCornerDp: Int = resolveVideoSharedTransitionSourceCornerDp(sourceRoute),
    playbackIntent: VideoSharedTransitionPlaybackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
    fullscreen: Boolean = false,
    autoPortrait: Boolean = false,
    initialVertical: Boolean = false,
    isVerticalVideo: Boolean = false,
    isReturning: Boolean = false,
    playerCornerDp: Int = DEFAULT_VIDEO_PLAYER_CORNER_DP,
    adaptiveInfo: VideoTransitionAdaptiveInfo = VideoTransitionAdaptiveInfo(),
): VideoSharedTransitionVisualSpec {
    val normalizedSourceRoute = sourceRoute?.substringBefore("?")?.takeIf { it.isNotBlank() }
    val safeSourceCornerDp = sourceCornerDp.coerceAtLeast(0)
    val shouldPreferPortraitTarget = initialVertical || (autoPortrait && isVerticalVideo)
    val targetMode = when {
        isReturning -> VideoSharedTransitionTargetMode.InlineCover
        shouldPreferPortraitTarget -> VideoSharedTransitionTargetMode.PortraitFullscreen
        playbackIntent == VideoSharedTransitionPlaybackIntent.CoverFirst ->
            VideoSharedTransitionTargetMode.InlineCover
        fullscreen -> VideoSharedTransitionTargetMode.LandscapeFullscreen
        else -> VideoSharedTransitionTargetMode.InlinePlayer
    }
    val targetCornerDp = when {
        // 返回：播放器常态仍用自身圆角；shell / 内层封面的实际裁切由返回策略统一到来源卡。
        isReturning -> playerCornerDp.coerceAtLeast(0)
        targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen -> 0
        targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen -> 0
        else -> playerCornerDp.coerceAtLeast(0)
    }
    val shouldUseCoverSharedBounds = when (adaptiveInfo.foldPosture) {
        AdaptiveFoldPosture.Book, AdaptiveFoldPosture.Tabletop -> false
        AdaptiveFoldPosture.None, AdaptiveFoldPosture.Flat ->
            normalizedSourceRoute != null &&
                !shouldSkipVideoCardSharedBoundsMorph(normalizedSourceRoute)
    }

    return VideoSharedTransitionVisualSpec(
        targetMode = targetMode,
        sourceCornerDp = safeSourceCornerDp,
        targetCornerDp = targetCornerDp,
        fillTargetViewport = targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen ||
            targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen,
        useCoverSharedBounds = shouldUseCoverSharedBounds,
        suppressCoverFade = isReturning
    )
}



/**
 * 详情壳 sharedBounds OverlayClip 圆角。
 * 返回全程保持来源卡的裁切形状，避免 shared overlay 卸层的最后一帧
 * 从直角突然切回卡片圆角。播放器、驻留封面与来源卡因此在同一形状下交接。
 */
internal fun resolveVideoDetailShellOverlayCornerDp(
    visualSpec: VideoSharedTransitionVisualSpec,
    liveReturnMorph: Boolean,
    isReturningVisualState: Boolean,
    playerCornerDp: Int = DEFAULT_VIDEO_PLAYER_CORNER_DP,
): Int {
    // live surface 返回也沿用来源卡圆角；不要在回程开始时切直角、落位时再切圆角。
    if (isReturningVisualState) {
        return if (liveReturnMorph) {
            visualSpec.sourceCornerDp.coerceAtLeast(0)
        } else {
            playerCornerDp.coerceAtLeast(0)
        }
    }
    return visualSpec.targetCornerDp.coerceAtLeast(0)
}

private fun resolveVideoSharedTransitionProfile(sourceRoute: String?): VideoSharedTransitionProfile {
    return if (sourceRoute?.substringBefore("?") == HOME_SOURCE_ROUTE) {
        VideoSharedTransitionProfile.COVER_ONLY
    } else {
        VideoSharedTransitionProfile.COVER_AND_METADATA
    }
}

internal fun shouldEnableVideoCoverSharedTransition(
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

/** 所有已支持的视频来源都保留共享 morph。 */
internal fun shouldSkipVideoCardSharedBoundsMorph(sourceRoute: String?): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return false
}

/**
 * cover relay 会让预测返回只剩孤立封面，无法保留真实上一页。
 * 横条卡改用封面尺寸的透明 shell 锚点，因此仍由 shell 保持页面所有权。
 */
internal fun shouldUseVideoCoverRelayTransition(sourceRoute: String?): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return false
}

internal fun shouldUseVideoCardShellSharedBounds(
    sourceRoute: String?,
    transitionEnabled: Boolean
): Boolean {
    if (!transitionEnabled) return false
    if (shouldSkipVideoCardSharedBoundsMorph(sourceRoute)) return false
    if (shouldUseVideoCoverRelayTransition(sourceRoute)) return false
    return !sourceRoute?.substringBefore("?").isNullOrBlank()
}

internal fun shouldUseHomeVideoCardShellContainerTransform(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return shouldUseVideoCardShellContainerTransform(
        sourceRoute = sourceRoute,
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = hasSharedTransitionScope,
        hasAnimatedVisibilityScope = hasAnimatedVisibilityScope
    )
}

internal fun shouldUseVideoCardShellContainerTransform(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    if (!transitionEnabled || !hasSharedTransitionScope || !hasAnimatedVisibilityScope) return false
    if (shouldSkipVideoCardSharedBoundsMorph(sourceRoute)) return false
    if (shouldUseVideoCoverRelayTransition(sourceRoute)) return false
    val normalizedSourceRoute = sourceRoute?.substringBefore("?")
    return isVideoCardReturnTargetRoute(normalizedSourceRoute)
}

internal fun shouldEnableVideoMetadataSharedTransition(
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    useCardContainerSharedBounds: Boolean = false,
    profile: VideoSharedTransitionProfile = resolveVideoSharedTransitionProfile()
): Boolean {
    if (!coverSharedEnabled) return false
    // 卡片容器已经承载整体放大/回收时，标题、UP、统计等不要再各自抢独立 sharedBounds。
    if (useCardContainerSharedBounds) return false
    // Keep metadata linked during quick return to avoid cover-only snapback.
    if (isQuickReturnLimited && profile == VideoSharedTransitionProfile.COVER_ONLY) return false
    // Home 源也启用 metadata sharedBounds，标题/头像/UP名独立共享
    return true
}

internal fun resolveVideoSharedTransitionOwnership(
    sourceRoute: String?,
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    transitionEnabled: Boolean = true
): VideoSharedTransitionOwnership {
    if (!coverSharedEnabled) {
        return VideoSharedTransitionOwnership(
            useCoverSharedBounds = false,
            useMetadataSharedBounds = false,
            useCardContainerSharedBounds = false
        )
    }
    if (shouldSkipVideoCardSharedBoundsMorph(sourceRoute)) {
        return VideoSharedTransitionOwnership(
            useCoverSharedBounds = false,
            useMetadataSharedBounds = false,
            useCardContainerSharedBounds = false
        )
    }

    val useCardContainerSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = transitionEnabled
    )
    val coverRelay = shouldUseVideoCoverRelayTransition(sourceRoute)
    return VideoSharedTransitionOwnership(
        useCoverSharedBounds = true,
        useMetadataSharedBounds = !coverRelay &&
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = isQuickReturnLimited,
                useCardContainerSharedBounds = useCardContainerSharedBounds,
                profile = resolveVideoSharedTransitionProfile(sourceRoute)
            ),
        useCardContainerSharedBounds = useCardContainerSharedBounds
    )
}

internal fun resolveVideoCardSharedTransitionMotionSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    speedSettings: VideoSharedTransitionSpeedSettings = VideoSharedTransitionSpeedSettings(),
    isQuickReturn: Boolean = false,
    adaptiveInfo: VideoTransitionAdaptiveInfo = VideoTransitionAdaptiveInfo(),
): VideoSharedTransitionMotionSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    if (!enabled) {
        return VideoSharedTransitionMotionSpec(
            enabled = false,
            durationMillis = 0,
            fullscreenDurationMillis = 0,
            contentDelayMillis = 0,
            contentDurationMillis = 0,
            contentSlideOffsetDp = 0,
            contentInitialScale = 1f,
            enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
            returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
            spatialStiffness = VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS,
            enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
            returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
        )
    }
    val durationMillis = resolveVideoSharedTransitionDurationMillis(speedSettings, adaptiveInfo)

    return VideoSharedTransitionMotionSpec(
        enabled = true,
        durationMillis = durationMillis,
        fullscreenDurationMillis = resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis),
        contentDelayMillis = if (isQuickReturn) {
            0
        } else {
            resolveVideoSharedTransitionContentDelayMillis(durationMillis)
        },
        contentDurationMillis = resolveVideoSharedTransitionContentDurationMillis(durationMillis),
        contentSlideOffsetDp = HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP,
        contentInitialScale = HOME_DETAIL_REVEAL_INITIAL_SCALE,
        enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
        returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
        spatialStiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis),
        enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
        returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
    )
}

internal fun resolveVideoMetadataSharedTransitionMotionSpec(
    transitionEnabled: Boolean,
    speedSettings: VideoSharedTransitionSpeedSettings = VideoSharedTransitionSpeedSettings(),
    adaptiveInfo: VideoTransitionAdaptiveInfo = VideoTransitionAdaptiveInfo(),
): VideoSharedTransitionMotionSpec {
    val durationMillis = if (transitionEnabled) {
        resolveVideoSharedTransitionDurationMillis(speedSettings, adaptiveInfo)
    } else {
        0
    }
    return VideoSharedTransitionMotionSpec(
        enabled = transitionEnabled,
        durationMillis = durationMillis,
        fullscreenDurationMillis = if (transitionEnabled) {
            resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis)
        } else {
            0
        },
        contentDelayMillis = 0,
        contentDurationMillis = durationMillis,
        contentSlideOffsetDp = 0,
        contentInitialScale = 1f,
        enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
        returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
        spatialStiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis),
        enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
        returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
    )
}

/**
 * Hero 空间曲线：
 * - 进场（卡片→详情）：Continuity，先快后慢
 * - 返回（详情→卡片）：Linear，保证预测返回 seek 与手指进度 1:1，
 *   松手后 remainingDuration 可按固定时长推算，避免 soft spring 导致
 *   「划一半有特效、松手一闪落位」以及完整加载后返回动画被瞬时掐掉
 *
 * 景深 / alpha 仍用 Continuity，落位柔和感由它们承担。
 */
internal fun resolveVideoSharedElementSpatialEasing(
    initialBounds: Rect,
    targetBounds: Rect,
): Easing {
    return when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> resolveVideoCardSharedTransitionSpatialEasing()
        VideoSharedTransitionDirection.RETURN -> LinearEasing
    }
}

/**
 * 遗留 API：返回 soft spring。
 * 主路径 [videoSharedElementBoundsTransformSpec] 已改回固定时长 tween，
 * 以保证 Navigation3 预测返回 seek/complete 的 remainingDuration 可计算。
 */
internal fun videoSharedElementReturnSpringSpec(
    motion: VideoSharedTransitionMotionSpec,
    durationMillis: Int = motion.durationMillis,
): SpringSpec<Rect> {
    return spring(
        dampingRatio = motion.returnSpatialDampingRatio,
        stiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis),
        visibilityThreshold = VIDEO_CARD_HERO_BOUNDS_VISIBILITY_THRESHOLD,
    )
}

/**
 * 返回 morph 固定时长 tween（Linear）：
 * - 可 seek：预测拖动 progress 与 bounds 进度一致
 * - 可打断：OPENING 中途返回可从当前 fraction 反转
 * - 松手后 remainingDuration = (1 - fraction) * duration，不再因 spring 无固定时长而 snap
 */
internal fun videoSharedElementReturnTweenSpec(
    durationMillis: Int,
    interactive: Boolean = true,
): FiniteAnimationSpec<Rect> {
    val hero = resolveVideoHeroMotionSpec(durationMillis)
    return tween(
        durationMillis = if (interactive) hero.enterDurationMillis else hero.returnDurationMillis,
        easing = if (interactive) hero.predictiveSeekSpec else hero.returnSpatialSpec,
    )
}

internal fun videoSharedElementBoundsTransformSpec(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect,
    durationMillis: Int = motion.durationMillis,
    // Legacy sharedBounds callers use seekable AVS; the Miuix path does not mount them.
    interactive: Boolean = true,
): FiniteAnimationSpec<Rect> {
    val hero = resolveVideoHeroMotionSpec(durationMillis)
    return when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> tween(
            durationMillis = hero.enterDurationMillis,
            easing = hero.enterSpatialSpec,
        )
        VideoSharedTransitionDirection.RETURN -> videoSharedElementReturnTweenSpec(
            durationMillis = durationMillis,
            interactive = interactive,
        )
    }
}

internal fun videoMetadataSharedElementBoundsTransformSpec(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect
): FiniteAnimationSpec<Rect> {
    val durationMillis = resolveVideoMetadataSharedBoundsDurationMillis(motion)
    return when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> tween(
            durationMillis = durationMillis,
            easing = resolveVideoCardSharedTransitionSpatialEasing(),
        )
        VideoSharedTransitionDirection.RETURN -> videoSharedElementReturnTweenSpec(
            durationMillis = durationMillis,
        )
    }
}

internal fun resolveVideoMetadataSharedBoundsDurationMillis(
    motion: VideoSharedTransitionMotionSpec
): Int {
    if (!motion.enabled) return 0
    return (motion.durationMillis * VIDEO_METADATA_SHARED_BOUNDS_RATIO)
        .roundToInt()
        .coerceIn(
            VIDEO_METADATA_SHARED_BOUNDS_MIN_MILLIS,
            VIDEO_METADATA_SHARED_BOUNDS_MAX_MILLIS
        )
}

internal fun resolveHomeVideoSharedTransitionCornerSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoSharedCornerSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    return if (enabled) {
        VideoSharedCornerSpec(
            enabled = true,
            startCornerDp = HOME_SHARED_TRANSITION_CARD_CORNER_DP,
            endCornerDp = HOME_SHARED_TRANSITION_PLAYER_CORNER_DP
        )
    } else {
        VideoSharedCornerSpec(
            enabled = false,
            startCornerDp = 0,
            endCornerDp = 0
        )
    }
}
