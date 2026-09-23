package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.OrientationEventListener
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.window.layout.WindowMetricsCalculator
import kotlin.math.abs
import com.android.purebilibili.core.util.AppDisplayContext
import com.android.purebilibili.core.util.applyPlayerRequestedOrientation

import com.android.purebilibili.core.ui.setWindowNavigationBarColor
import com.android.purebilibili.core.ui.setWindowStatusBarColor
import com.android.purebilibili.core.ui.adaptive.AdaptiveFoldPosture

internal fun shouldUseVideoDetailSharedElementMorph(
    foldPosture: AdaptiveFoldPosture,
): Boolean = foldPosture != AdaptiveFoldPosture.Book &&
    foldPosture != AdaptiveFoldPosture.Tabletop

internal data class VideoDetailSystemBarsSnapshot(
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
    val systemBarsBehavior: Int
)

internal fun resolveVideoDetailSystemBarsSnapshot(
    statusBarColor: Int?,
    navigationBarColor: Int?,
    lightStatusBars: Boolean?,
    lightNavigationBars: Boolean?,
    systemBarsBehavior: Int?,
    fallbackColor: Int,
    fallbackLightBars: Boolean,
    fallbackSystemBarsBehavior: Int
): VideoDetailSystemBarsSnapshot {
    return VideoDetailSystemBarsSnapshot(
        statusBarColor = statusBarColor ?: fallbackColor,
        navigationBarColor = navigationBarColor ?: fallbackColor,
        lightStatusBars = lightStatusBars ?: fallbackLightBars,
        lightNavigationBars = lightNavigationBars ?: fallbackLightBars,
        systemBarsBehavior = systemBarsBehavior ?: fallbackSystemBarsBehavior
    )
}

internal fun shouldShowSystemBarsOnVideoDetailExit(): Boolean {
    return true
}

internal data class VideoDetailSystemBarsVisibilityPolicy(
    val hideStatusBars: Boolean,
    val hideNavigationBars: Boolean
)

internal enum class VideoDetailHiddenSystemBars {
    NONE,
    STATUS_BARS,
    SYSTEM_BARS
}

internal data class VideoDetailSystemBarsApplySpec(
    val hiddenBars: VideoDetailHiddenSystemBars,
    val systemBarsBehavior: Int,
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean
)

@Suppress("UNUSED_PARAMETER")
internal fun resolveVideoDetailSystemBarsVisibilityPolicy(
    isFullscreenMode: Boolean,
    hideVideoPageStatusBar: Boolean,
    isInPipMode: Boolean,
    isScreenActive: Boolean,
    isPortraitFullscreen: Boolean = false,
    forceShowSystemBarsInPortrait: Boolean = false
): VideoDetailSystemBarsVisibilityPolicy {
    if (!isScreenActive || isInPipMode) {
        return VideoDetailSystemBarsVisibilityPolicy(
            hideStatusBars = false,
            hideNavigationBars = false
        )
    }
    // Portrait immersive pager: hide status + nav bars for full-bleed playback.
    // forceShow allows the portrait chrome toggle to temporarily restore bars.
    if (isPortraitFullscreen) {
        if (forceShowSystemBarsInPortrait) {
            return VideoDetailSystemBarsVisibilityPolicy(
                hideStatusBars = false,
                hideNavigationBars = false
            )
        }
        return VideoDetailSystemBarsVisibilityPolicy(
            hideStatusBars = true,
            hideNavigationBars = true
        )
    }
    if (isFullscreenMode) {
        return VideoDetailSystemBarsVisibilityPolicy(
            hideStatusBars = true,
            hideNavigationBars = true
        )
    }
    // This preference used to hide the status bar. It now controls the Compose Haze backdrop
    // above the inline player, so the system icons remain visible and readable.
    return VideoDetailSystemBarsVisibilityPolicy(
        hideStatusBars = false,
        hideNavigationBars = false
    )
}

internal fun resolveVideoDetailSystemBarsApplySpec(
    visibilityPolicy: VideoDetailSystemBarsVisibilityPolicy,
    useTabletLayout: Boolean,
    isLightBackground: Boolean,
    useCollapsedPlayerChromeAppearance: Boolean = false,
    backgroundColor: Int,
    transparentColor: Int,
    blackColor: Int,
    transientBarsBehavior: Int
): VideoDetailSystemBarsApplySpec {
    if (visibilityPolicy.hideNavigationBars) {
        return VideoDetailSystemBarsApplySpec(
            hiddenBars = VideoDetailHiddenSystemBars.SYSTEM_BARS,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = blackColor,
            navigationBarColor = blackColor,
            lightStatusBars = false,
            lightNavigationBars = false
        )
    }

    val hiddenBars = if (visibilityPolicy.hideStatusBars) {
        VideoDetailHiddenSystemBars.STATUS_BARS
    } else {
        VideoDetailHiddenSystemBars.NONE
    }
    return if (useTabletLayout) {
        VideoDetailSystemBarsApplySpec(
            hiddenBars = hiddenBars,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = backgroundColor,
            navigationBarColor = backgroundColor,
            lightStatusBars = isLightBackground,
            lightNavigationBars = isLightBackground
        )
    } else {
        VideoDetailSystemBarsApplySpec(
            hiddenBars = hiddenBars,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = transparentColor,
            navigationBarColor = transparentColor,
            lightStatusBars = useCollapsedPlayerChromeAppearance && isLightBackground,
            lightNavigationBars = false
        )
    }
}

internal fun resolveVideoDetailStableStatusBarHeightDp(
    visibleStatusBarHeightDp: Float,
    statusBarIgnoringVisibilityHeightDp: Float,
    hideStatusBars: Boolean
): Float {
    fun sanitize(value: Float): Float {
        return value.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    }

    val visibleInset = sanitize(visibleStatusBarHeightDp)
    val stableInset = sanitize(statusBarIgnoringVisibilityHeightDp)
    return if (hideStatusBars) {
        stableInset.coerceAtLeast(visibleInset)
    } else {
        visibleInset
    }
}

/**
 * 竖屏详情播放器顶部沉浸带高度。
 *
 * 详情页内联播放器始终沉浸：视频内容延伸到系统状态栏后方，状态栏区域由背景条
 * （「播放页沉浸状态栏」开关：实时模糊 或 纯黑）填充。因此只要状态栏可见即返回
 * 状态栏高度；[immersiveStatusBarBackdropEnabled] 仅保留为兼容参数。
 *
 * [isSharedCardTransition] 保留参数兼容；共享转场与落位后的几何保持一致，
 * 避免动画结束时播放器高度突然跳变。
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolveVideoDetailPortraitPlayerTopInsetDp(
    stableStatusBarHeightDp: Float,
    hideStatusBars: Boolean,
    immersiveStatusBarBackdropEnabled: Boolean = false,
    isSharedCardTransition: Boolean = false,
): Float {
    if (hideStatusBars) return 0f
    return stableStatusBarHeightDp
        .takeIf { it.isFinite() }
        ?.coerceAtLeast(0f)
        ?: 0f
}

/**
 * 播放器顶部控件是否应避让系统状态栏。
 *
 * - 状态栏可见（普通详情和「播放页沉浸状态栏」）→ 必须 padding，防重叠
 * - 状态栏已隐藏（横屏全屏 / 竖屏全屏）→ 不 padding，保持贴顶沉浸
 */
internal fun shouldApplyStatusBarPaddingToVideoPlayerChrome(
    statusBarVisible: Boolean,
): Boolean = statusBarVisible

internal fun shouldRestoreSystemBarsDuringVideoDetailExitTransition(
    isExitTransitionInProgress: Boolean,
    isActuallyLeaving: Boolean
): Boolean {
    if (!isExitTransitionInProgress) return false
    if (isActuallyLeaving) return false
    return true
}

/**
 * 预测返回手势开始退出时会提前 restore 状态栏；若手势取消、详情仍留在栈顶，
 * 需重新激活沉浸式，否则会一直停在非沉浸（常见为黑底状态栏）。
 */
internal fun shouldReactivateVideoDetailSystemBarsAfterCancelledExit(
    isExitTransitionInProgress: Boolean,
    isActuallyLeaving: Boolean,
    isScreenActive: Boolean,
): Boolean {
    if (isExitTransitionInProgress) return false
    if (isActuallyLeaving) return false
    return !isScreenActive
}

/** 从相关视频等上层页预测返回后，底层详情重新成为栈顶时需强制重套系统栏。 */
internal fun shouldReapplyVideoDetailSystemBarsAfterBecomingTop(
    wasKeepLoadedContentForBackPreview: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    isActuallyLeaving: Boolean,
): Boolean {
    if (isActuallyLeaving) return false
    return wasKeepLoadedContentForBackPreview && !keepLoadedContentForBackPreview
}

internal fun applyVideoDetailSystemBarsSpec(
    window: Window,
    insetsController: WindowInsetsControllerCompat,
    spec: VideoDetailSystemBarsApplySpec
) {
    when (spec.hiddenBars) {
        VideoDetailHiddenSystemBars.SYSTEM_BARS -> {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        VideoDetailHiddenSystemBars.STATUS_BARS -> {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
        VideoDetailHiddenSystemBars.NONE -> {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    insetsController.systemBarsBehavior = spec.systemBarsBehavior
    insetsController.isAppearanceLightStatusBars = spec.lightStatusBars
    insetsController.isAppearanceLightNavigationBars = spec.lightNavigationBars
    setWindowStatusBarColor(window, spec.statusBarColor)
    setWindowNavigationBarColor(window, spec.navigationBarColor)
}


internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

internal fun isWindowBoundsSmallerThanMaximum(
    currentWidth: Int,
    currentHeight: Int,
    maximumWidth: Int,
    maximumHeight: Int,
    tolerancePx: Int = 4
): Boolean {
    if (currentWidth <= 0 || currentHeight <= 0 || maximumWidth <= 0 || maximumHeight <= 0) {
        return false
    }
    return currentWidth + tolerancePx < maximumWidth ||
        currentHeight + tolerancePx < maximumHeight
}

internal fun shouldInferFloatingWindowFromBounds(
    currentBoundsSmallerThanMaximum: Boolean,
    isFoldableCoverWindow: Boolean,
): Boolean = currentBoundsSmallerThanMaximum && !isFoldableCoverWindow

/**
 * 部分 vivo/iQOO 系统悬浮窗不会稳定上报 [Activity.isInMultiWindowMode]。
 * 此时以当前窗口小于最大可用窗口作为兜底，避免把浮窗误当成普通全屏 Activity，
 * 继而写入横屏方向请求并触发系统窗口瞬时展开后回弹。
 */
internal fun isActivityInMultiWindowOrFloatingMode(
    activity: Activity,
    displayContext: AppDisplayContext? = null,
): Boolean {
    // PiP 有独立播放与方向策略，不能仅因窗口边界较小而归入普通悬浮窗。
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.isInPictureInPictureMode) {
        return false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode) {
        return true
    }

    return runCatching {
        val calculator = WindowMetricsCalculator.getOrCreate()
        val currentBounds = calculator.computeCurrentWindowMetrics(activity).bounds
        val maximumBounds = calculator.computeMaximumWindowMetrics(activity).bounds
        val isCoverWindow = displayContext?.isFoldableCoverWindow == true
        shouldInferFloatingWindowFromBounds(
            currentBoundsSmallerThanMaximum = isWindowBoundsSmallerThanMaximum(
                currentWidth = currentBounds.width(),
                currentHeight = currentBounds.height(),
                maximumWidth = maximumBounds.width(),
                maximumHeight = maximumBounds.height()
            ),
            isFoldableCoverWindow = isCoverWindow,
        )
    }.getOrDefault(false)
}

internal fun toggleVideoDetailFullscreen(
    activity: Activity?,
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    isFullscreenMode: Boolean,
    isCompactDevice: Boolean,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isVerticalVideo: Boolean,
    preferPortraitForFlatFoldable: Boolean = false,
    displayContext: AppDisplayContext? = null,
    portraitExperienceEnabled: Boolean,
    onEnterPortraitFullscreen: () -> Unit,
    onUserRequestedFullscreenChange: (Boolean) -> Unit,
    onManualPortraitHoldActiveChange: (Boolean) -> Unit
) {
    if (activity == null) return

    val isInMultiWindowMode = isActivityInMultiWindowOrFloatingMode(
        activity = activity,
        displayContext = displayContext,
    )
    val isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        activity.isInPictureInPictureMode
    if (shouldUseInWindowFullscreenForSystemMultiWindow(
            isInMultiWindowMode = isInMultiWindowMode,
            isInPictureInPictureMode = isInPictureInPictureMode,
            isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
            isFullscreenMode = isFullscreenMode
        )
    ) {
        onUserRequestedFullscreenChange(true)
        onManualPortraitHoldActiveChange(false)
        return
    }

    if (isOrientationDrivenFullscreen && isInMultiWindowMode && isFullscreenMode) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(false)
        return
    }

    if (!isOrientationDrivenFullscreen) {
        val nextRequestedFullscreen = !isFullscreenMode
        onUserRequestedFullscreenChange(nextRequestedFullscreen)
        if (!nextRequestedFullscreen) {
            if (isCompactDevice && fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL) {
                activity.applyPlayerRequestedOrientation(
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    displayContext = displayContext,
                )
            } else if (isLandscapeRequestedOrientation(activity.requestedOrientation)) {
                activity.applyPlayerRequestedOrientation(
                    requestedOrientation = if (isCompactDevice) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    displayContext = displayContext,
                )
            }
        }
        return
    }

    if (isFullscreenMode) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(isLandscape)
        activity.applyPlayerRequestedOrientation(
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            displayContext = displayContext,
        )
        return
    }

    val targetOrientation = resolvePhoneFullscreenEnterOrientation(
        fullscreenMode = fullscreenMode,
        isVerticalVideo = isVerticalVideo,
        preferPortraitForFlatFoldable = preferPortraitForFlatFoldable
    ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    if (shouldEnterPortraitFullscreenOnFullscreenToggle(
            targetOrientation = targetOrientation,
            portraitExperienceEnabled = portraitExperienceEnabled
        )
    ) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(false)
        onEnterPortraitFullscreen()
        return
    }

    onUserRequestedFullscreenChange(true)
    onManualPortraitHoldActiveChange(false)
    activity.applyPlayerRequestedOrientation(
        requestedOrientation = targetOrientation,
        displayContext = displayContext,
    )
}

internal fun resolveNextPlayerHeightOffset(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    maxOffsetPx: Float = 0f,
    minUpdateDeltaPx: Float = 0.75f
): Float? {
    if (abs(deltaPx) < minUpdateDeltaPx) return null
    val nextOffset = (currentOffsetPx + deltaPx).coerceIn(minOffsetPx, maxOffsetPx)
    return if (abs(nextOffset - currentOffsetPx) < minUpdateDeltaPx) {
        null
    } else {
        nextOffset
    }
}

internal fun resolveIsPlayerCollapsed(
    swipeHidePlayerEnabled: Boolean,
    playerHeightOffsetPx: Float,
    videoHeightPx: Float,
    collapseTolerancePx: Float = 10f
): Boolean {
    if (!swipeHidePlayerEnabled) return false
    return playerHeightOffsetPx <= (-videoHeightPx + collapseTolerancePx)
}

internal fun resolveIsPlaybackPausedForCollapse(
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    // 播放结束后 Media3 可能仍保留 playWhenReady=true，但对折叠交互来说它已是静止态。
    // 其余情况仍按用户暂停意图判断，避免把缓冲态误判为“暂停时可缩小”。
    return playbackState == Player.STATE_ENDED || !playWhenReady
}

internal fun shouldUseTabletVideoLayout(
    isExpandedScreen: Boolean,
    isTabletDevice: Boolean
): Boolean {
    return isExpandedScreen && isTabletDevice
}

internal fun shouldUseOrientationDrivenFullscreen(
    isCompactDevice: Boolean
): Boolean {
    return isCompactDevice
}

internal fun shouldRotateToPortraitOnSplitBack(
    useTabletLayout: Boolean,
    isCompactDevice: Boolean,
    orientation: Int
): Boolean {
    return useTabletLayout && isCompactDevice && orientation == Configuration.ORIENTATION_LANDSCAPE
}

internal fun shouldShowDetachedVideoCommentThreadHost(
    useTabletLayout: Boolean
): Boolean {
    return !useTabletLayout
}

internal fun resolveVideoDetailCommentThreadHostMainSheetVisible(
    useEmbeddedPresentation: Boolean,
    subReplyVisible: Boolean
): Boolean {
    // 楼中楼直接覆盖在视频详情页上方，不额外铺设一层“主评论列表”，
    // 从而在预测返回手势过程中直接露出底层的原视频详情页（保留“简介/评论/发弹幕”栏与原本的滚动位置）。
    return false
}

internal fun shouldForceInitializeDetachedCommentThreadHostForRoute(
    routeCommentRootRpid: Long,
    aid: Long,
    hasHandledRouteComment: Boolean
): Boolean {
    return routeCommentRootRpid > 0L && aid > 0L && !hasHandledRouteComment
}

internal fun shouldApplyPhoneAutoRotatePolicy(
    isCompactDevice: Boolean
): Boolean {
    return isCompactDevice
}

internal fun resolveVideoDetailFullscreenMode(
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    userRequestedFullscreen: Boolean,
    isInMultiWindowMode: Boolean
): Boolean {
    if (!isOrientationDrivenFullscreen) return userRequestedFullscreen
    return isLandscape || (isInMultiWindowMode && userRequestedFullscreen)
}

internal fun shouldApplyStartFullscreenOrientationRequest(
    startInFullscreen: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    isInMultiWindowMode: Boolean
): Boolean {
    if (!startInFullscreen) return false
    if (!isOrientationDrivenFullscreen) return false
    if (isLandscape) return false
    // 系统小窗/分屏内强写 requestedOrientation 会让部分 ROM 在横竖窗口间反复重建。
    if (isInMultiWindowMode) return false
    return true
}

internal fun resolvePhoneFullscreenEnterOrientation(
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isVerticalVideo: Boolean,
    preferPortraitForFlatFoldable: Boolean = false
): Int? {
    return when (fullscreenMode) {
        com.android.purebilibili.core.store.FullscreenMode.NONE -> null
        com.android.purebilibili.core.store.FullscreenMode.VERTICAL -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        com.android.purebilibili.core.store.FullscreenMode.HORIZONTAL -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        com.android.purebilibili.core.store.FullscreenMode.AUTO -> {
            if (isVerticalVideo || preferPortraitForFlatFoldable) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
}

internal fun shouldKeepManualFullscreenRequest(
    manualFullscreenRequested: Boolean,
    hasEnteredFullscreenDuringRequest: Boolean,
    isFullscreenMode: Boolean
): Boolean {
    if (!manualFullscreenRequested) return false
    if (isFullscreenMode) return true
    return !hasEnteredFullscreenDuringRequest
}

@Composable
internal fun ManualFullscreenRequestLifecycleEffect(
    manualFullscreenRequested: Boolean,
    isFullscreenMode: Boolean,
    requestEnvironmentKey: Any? = Unit,
    onReleaseManualFullscreenRequest: () -> Unit
) {
    var hasEnteredFullscreenDuringRequest by rememberSaveable(requestEnvironmentKey) {
        mutableStateOf(false)
    }
    val latestOnReleaseManualFullscreenRequest by rememberUpdatedState(
        onReleaseManualFullscreenRequest
    )

    LaunchedEffect(requestEnvironmentKey, manualFullscreenRequested, isFullscreenMode) {
        if (manualFullscreenRequested && isFullscreenMode) {
            hasEnteredFullscreenDuringRequest = true
            return@LaunchedEffect
        }

        if (
            !shouldKeepManualFullscreenRequest(
                manualFullscreenRequested = manualFullscreenRequested,
                hasEnteredFullscreenDuringRequest = hasEnteredFullscreenDuringRequest,
                isFullscreenMode = isFullscreenMode
            )
        ) {
            if (manualFullscreenRequested) {
                latestOnReleaseManualFullscreenRequest()
            }
            hasEnteredFullscreenDuringRequest = false
        }
    }
}

internal fun resolvePhoneVideoRequestedOrientation(
    autoRotateEnabled: Boolean,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isCompactDevice: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isFullscreenMode: Boolean,
    manualFullscreenRequested: Boolean = false,
    manualPortraitHoldActive: Boolean = false,
    isVerticalVideo: Boolean = false,
    isPortraitFullscreen: Boolean = false,
    currentRequestedOrientation: Int? = null,
    isInMultiWindowMode: Boolean = false,
    isInPictureInPictureMode: Boolean = false,
    preferPortraitForFlatFoldable: Boolean = false,
    preserveExactLandscapeSide: Boolean = true,
    isCurrentlyLandscape: Boolean = false,
): Int? {
    // A size class alone can classify a tablet or a large phone as a foldable. Keep this
    // preference out of compact layouts even if an upstream caller misclassifies the device.
    val preferPortraitForFoldableInnerScreen =
        !isCompactDevice && preferPortraitForFlatFoldable
    // Player/API dimensions describe the encoded video, not the requested device posture. On a
    // tablet they must never turn a manual landscape fullscreen request into portrait (metadata
    // may be stale or rotated). Video-directed orientation remains a phone-only behavior.
    val isVerticalVideoForOrientation = isCompactDevice && isVerticalVideo
    if (isInMultiWindowMode || isInPictureInPictureMode) {
        return null
    }
    // 竖屏刷视频是独立沉浸体验，不在这里写入 requestedOrientation。
    if (isPortraitFullscreen) {
        return null
    }
    if (!shouldApplyPhoneAutoRotatePolicy(isCompactDevice)) {
        return when {
            isFullscreenMode || manualFullscreenRequested -> {
                val fullscreenOrientation = resolvePhoneFullscreenEnterOrientation(
                    fullscreenMode = fullscreenMode,
                    isVerticalVideo = isVerticalVideoForOrientation,
                    preferPortraitForFlatFoldable = preferPortraitForFoldableInnerScreen
                )
                resolveStableOrientationWhenAutoRotateDisabled(
                    requestedOrientation = fullscreenOrientation,
                    currentRequestedOrientation = currentRequestedOrientation,
                    autoRotateEnabled = autoRotateEnabled
                )
            }
            autoRotateEnabled -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.NONE) {
        return null
    }
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL) {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    if (!isOrientationDrivenFullscreen) {
        return null
    }
    if (manualPortraitHoldActive) {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    if (resolveEffectivePhoneAutoRotateEnabled(
            autoRotateEnabled = autoRotateEnabled,
            manualPortraitHoldActive = manualPortraitHoldActive
        )
    ) {
        return when {
            manualFullscreenRequested -> {
                val fullscreenOrientation = resolvePhoneFullscreenEnterOrientation(
                    fullscreenMode = fullscreenMode,
                    isVerticalVideo = isVerticalVideo,
                    preferPortraitForFlatFoldable = preferPortraitForFoldableInnerScreen
                ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                preserveCurrentExactLandscapeSideWhileFullscreen(
                    requestedOrientation = fullscreenOrientation,
                    currentRequestedOrientation = currentRequestedOrientation,
                    isFullscreenMode = isFullscreenMode,
                    preserveExactLandscapeSide = preserveExactLandscapeSide,
                )
            }
            // Preserve the listener's physical side across fullscreen configuration updates.
            // Releasing it to SENSOR_LANDSCAPE can snap back on phone ROMs too.
            isFullscreenMode -> preserveCurrentExactLandscapeSideWhileFullscreen(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                currentRequestedOrientation = currentRequestedOrientation,
                isFullscreenMode = true,
                preserveExactLandscapeSide = preserveExactLandscapeSide,
            )
            // Configuration describes the current window orientation. Display rotation is
            // relative to the display's natural orientation and cannot identify a fixed
            // landscape side across foldables and vendor rotation configurations. Keep
            // sensor ownership until the orientation listener has selected an exact side.
            isCurrentlyLandscape ->
                resolveCurrentExactLandscapeOrientation(currentRequestedOrientation)
                    ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    return if (isFullscreenMode || manualFullscreenRequested) {
        val fullscreenOrientation = resolvePhoneFullscreenEnterOrientation(
            fullscreenMode = fullscreenMode,
            isVerticalVideo = isVerticalVideo,
            preferPortraitForFlatFoldable = preferPortraitForFoldableInnerScreen
        ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        resolveStableOrientationWhenAutoRotateDisabled(
            requestedOrientation = fullscreenOrientation,
            currentRequestedOrientation = currentRequestedOrientation,
            autoRotateEnabled = autoRotateEnabled
        )
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

private fun resolveStableOrientationWhenAutoRotateDisabled(
    requestedOrientation: Int?,
    currentRequestedOrientation: Int?,
    autoRotateEnabled: Boolean
): Int? {
    if (
        autoRotateEnabled ||
        requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ) {
        return requestedOrientation
    }
    // SENSOR_LANDSCAPE still follows gravity and can oscillate between both landscape sides
    // while the user is lying down. With app auto-rotate disabled, preserve an exact landscape
    // side if one is already active; otherwise enter the platform's fixed landscape orientation.
    return resolveCurrentExactLandscapeOrientation(currentRequestedOrientation)
        ?: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

internal fun resolvePhoneAutoRotateRequestedOrientation(
    orientationDegrees: Int,
    isCurrentlyLandscape: Boolean,
    useExactLandscapeSide: Boolean = true,
    allowPortraitTransitions: Boolean = true,
    portraitSnapDegrees: Int = 25,
    landscapeEnterMinDegrees: Int = 60,
    landscapeEnterMaxDegrees: Int = 120,
    landscapeKeepMinDegrees: Int = 40,
    landscapeKeepMaxDegrees: Int = 140
): Int? {
    if (orientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return null
    val normalized = ((orientationDegrees % 360) + 360) % 360

    val uprightPortraitStable = isPhoneOrientationUprightPortraitStable(
        orientationDegrees = normalized,
        portraitSnapDegrees = portraitSnapDegrees
    )
    val portraitStable = isPhoneOrientationPortraitStable(
        orientationDegrees = normalized,
        portraitSnapDegrees = portraitSnapDegrees
    )
    val exactLandscapeEntry = resolveExactLandscapeOrientation(
        orientationDegrees = normalized,
        minLeftSideTopDegrees = landscapeEnterMinDegrees,
        maxLeftSideTopDegrees = landscapeEnterMaxDegrees,
        minRightSideTopDegrees = 240,
        maxRightSideTopDegrees = 300
    )
    val exactLandscapeKeep = resolveExactLandscapeOrientation(
        orientationDegrees = normalized,
        minLeftSideTopDegrees = landscapeKeepMinDegrees,
        maxLeftSideTopDegrees = landscapeKeepMaxDegrees,
        minRightSideTopDegrees = 220,
        maxRightSideTopDegrees = 320
    )

    return when {
        // Track the physical side on phones and foldable covers. Some ROMs stop following
        // SENSOR_LANDSCAPE after entry; exact requests keep both sides reachable.
        isCurrentlyLandscape && exactLandscapeKeep != null -> if (useExactLandscapeSide) {
            exactLandscapeKeep
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        // Already landscape: only 0° exits to portrait. 180° is the midpoint of a
        // landscape-to-landscape flip; treating it as portrait makes cover screens
        // recognize the other side, then snap back to the original 90° landscape.
        isCurrentlyLandscape && uprightPortraitStable && allowPortraitTransitions ->
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        !isCurrentlyLandscape && portraitStable && allowPortraitTransitions ->
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        !isCurrentlyLandscape && exactLandscapeEntry != null -> if (useExactLandscapeSide) {
            exactLandscapeEntry
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        else -> null
    }
}

internal const val PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS = 500L

internal fun resolvePhoneAutoRotateTargetToApply(
    candidateOrientation: Int?,
    lastLandscapeAppliedAtMs: Long?,
    nowMs: Long,
    landscapeSettleMs: Long = PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS,
    lastPortraitAppliedAtMs: Long? = null,
    portraitSettleMs: Long = PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS,
): Int? {
    if (candidateOrientation == null) return null
    // 系统配置切到横屏有延迟；按最近一次横屏写入时间保护，避免刚进横屏又被残留竖屏角度拉回。
    if (
        candidateOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
        lastLandscapeAppliedAtMs != null &&
        nowMs - lastLandscapeAppliedAtMs < landscapeSettleMs
    ) {
        return null
    }
    // 反向保护：刚切到竖屏时，避免立即被残余角度或状态拉回横屏导致死循环。
    if (
        isLandscapeRequestedOrientation(candidateOrientation) &&
        lastPortraitAppliedAtMs != null &&
        nowMs - lastPortraitAppliedAtMs < portraitSettleMs
    ) {
        return null
    }
    return candidateOrientation
}

internal fun resolveCurrentExactLandscapeOrientation(
    currentRequestedOrientation: Int?,
): Int? {
    when (currentRequestedOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> return currentRequestedOrientation
    }
    return null
}

private fun preserveCurrentExactLandscapeSideWhileFullscreen(
    requestedOrientation: Int,
    currentRequestedOrientation: Int?,
    isFullscreenMode: Boolean,
    preserveExactLandscapeSide: Boolean,
): Int {
    if (
        !preserveExactLandscapeSide ||
        !isFullscreenMode ||
        requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ) {
        return requestedOrientation
    }
    return resolveCurrentExactLandscapeOrientation(
        currentRequestedOrientation = currentRequestedOrientation,
    ) ?: requestedOrientation
}

private fun resolveExactLandscapeOrientation(
    orientationDegrees: Int,
    minLeftSideTopDegrees: Int,
    maxLeftSideTopDegrees: Int,
    minRightSideTopDegrees: Int,
    maxRightSideTopDegrees: Int
): Int? {
    return when {
        withinWrappedRange(
            orientationDegrees,
            minLeftSideTopDegrees,
            maxLeftSideTopDegrees
        ) -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        withinWrappedRange(
            orientationDegrees,
            minRightSideTopDegrees,
            maxRightSideTopDegrees
        ) -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else -> null
    }
}

internal fun isLandscapeRequestedOrientation(requestedOrientation: Int): Boolean {
    return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
}

/**
 * 详情页/全屏路径会写入的横屏锁。连切相关视频时若把这类值当成「进入前方向」快照，
 * 退出会再次写回横屏，导致首页也横屏。
 */
internal fun isVideoDrivenLandscapeOrientationLock(requestedOrientation: Int): Boolean {
    return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
}

/**
 * 进入详情时记录「离开后应恢复」的方向。已是视频横屏锁时改记 UNSPECIFIED，
 * 避免旧详情 dispose 延迟恢复前、新详情把横屏锁当成入口快照。
 */
internal fun resolveVideoDetailEntryOrientationSnapshot(
    currentRequestedOrientation: Int?
): Int {
    val current = currentRequestedOrientation
        ?: return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    return if (isVideoDrivenLandscapeOrientationLock(current)) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        current
    }
}

internal fun shouldEnterPortraitFullscreenOnFullscreenToggle(
    targetOrientation: Int,
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled && targetOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

internal fun shouldUseInWindowFullscreenForSystemMultiWindow(
    isInMultiWindowMode: Boolean,
    isInPictureInPictureMode: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isFullscreenMode: Boolean
): Boolean {
    if (!isOrientationDrivenFullscreen) return false
    if (isFullscreenMode) return false
    if (isInPictureInPictureMode) return false
    return isInMultiWindowMode
}

internal fun resolvePortraitRotateTargetOrientation(
    isOrientationDrivenFullscreen: Boolean,
    manualPortraitHoldActive: Boolean = false
): Int? {
    return if (isOrientationDrivenFullscreen && !manualPortraitHoldActive) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        null
    }
}

internal fun resolveEffectivePhoneAutoRotateEnabled(
    autoRotateEnabled: Boolean,
    manualPortraitHoldActive: Boolean
): Boolean {
    return autoRotateEnabled && !manualPortraitHoldActive
}

/**
 * A fullscreen-button press may force landscape while the phone is still physically upright.
 * Do not interpret that already-existing portrait posture as a rotate-back gesture. Once the
 * sensor observes a real landscape posture, the manual intent is released and normal automatic
 * portrait exit is enabled again.
 */
internal fun shouldAllowPhoneSensorPortraitTransition(
    autoRotateEnabled: Boolean,
    manualFullscreenRequested: Boolean,
): Boolean = autoRotateEnabled && !manualFullscreenRequested

internal fun shouldReleaseManualFullscreenRequestAfterSensorTarget(
    manualFullscreenRequested: Boolean,
    sensorTargetOrientation: Int?,
): Boolean = manualFullscreenRequested &&
    sensorTargetOrientation != null &&
    isLandscapeRequestedOrientation(sensorTargetOrientation)

internal fun shouldObservePhoneAutoRotate(
    autoRotateEnabled: Boolean,
    isCompactDevice: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    manualPortraitHoldActive: Boolean,
    isInMultiWindowMode: Boolean = false,
    isInPictureInPictureMode: Boolean = false,
    isPortraitFullscreen: Boolean = false,
    observeWhenAutoRotateDisabled: Boolean = false,
    isFullscreenMode: Boolean = false,
): Boolean {
    // A cover display may be classified as a compact phone when its metrics refresh.
    // Keep tracking both landscape sides throughout fullscreen, regardless of that label.
    if (!autoRotateEnabled && !observeWhenAutoRotateDisabled && !isFullscreenMode) return false
    if (isInMultiWindowMode || isInPictureInPictureMode) return false
    // BiliPai-style: vertical immersive FS is not kicked by gravity / sensor landscape.
    if (isPortraitFullscreen) return false
    if (!shouldApplyPhoneAutoRotatePolicy(isCompactDevice)) return false
    if (!isOrientationDrivenFullscreen) return false
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.NONE) return false
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL) return false
    return true
}

internal fun shouldReleasePhoneManualPortraitHold(
    orientationDegrees: Int,
    portraitSnapDegrees: Int = 25
): Boolean {
    if (orientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return false
    return isPhoneOrientationPortraitStable(
        orientationDegrees = orientationDegrees,
        portraitSnapDegrees = portraitSnapDegrees
    )
}

private fun isPhoneOrientationUprightPortraitStable(
    orientationDegrees: Int,
    portraitSnapDegrees: Int
): Boolean {
    val normalized = ((orientationDegrees % 360) + 360) % 360
    return withinWrappedRange(normalized, 0, portraitSnapDegrees) ||
        withinWrappedRange(normalized, 360 - portraitSnapDegrees, 359)
}

private fun isPhoneOrientationPortraitStable(
    orientationDegrees: Int,
    portraitSnapDegrees: Int
): Boolean {
    val normalized = ((orientationDegrees % 360) + 360) % 360
    return isPhoneOrientationUprightPortraitStable(normalized, portraitSnapDegrees) ||
        withinWrappedRange(normalized, 180 - portraitSnapDegrees, 180 + portraitSnapDegrees)
}

private fun withinWrappedRange(
    value: Int,
    min: Int,
    max: Int
): Boolean {
    return if (min <= max) {
        value in min..max
    } else {
        value >= min || value <= max
    }
}

internal fun resolveVideoDetailExitRequestedOrientation(
    originalRequestedOrientation: Int?
): Int {
    val original = originalRequestedOrientation
        ?: return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    return if (isVideoDrivenLandscapeOrientationLock(original)) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        original
    }
}

internal fun shouldEnablePortraitExperience(): Boolean {
    return true
}

internal fun shouldShowVideoDetailBottomInteractionBar(
    useTabletLayout: Boolean,
    selectedTabIndex: Int,
    isFullscreenMode: Boolean,
    isPortraitFullscreen: Boolean,
    isCommentInputVisible: Boolean,
    isCommentThreadVisible: Boolean,
    isFavoriteFolderDialogVisible: Boolean,
    isExternalPlaylistQueueBarVisible: Boolean
): Boolean {
    return !useTabletLayout &&
        selectedTabIndex == VIDEO_CONTENT_COMMENT_TAB_INDEX &&
        !isFullscreenMode &&
        !isPortraitFullscreen &&
        !isCommentInputVisible &&
        !isFavoriteFolderDialogVisible &&
        !isExternalPlaylistQueueBarVisible
}

internal fun shouldShowVideoDetailActionButtons(): Boolean {
    return true
}

// VideoContentSection 已提取到 VideoContentSection.kt
// VideoTagsRow 和 VideoTagChip 也已提取到 VideoContentSection.kt
