package com.android.purebilibili.navigation3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.motion.SETTINGS_IOS_PUSH_DURATION_MS
import com.android.purebilibili.core.ui.motion.navigationSlideSpring
import com.android.purebilibili.core.ui.motion.resolveBottomBarLikeHorizontalContentTransform
import com.android.purebilibili.core.ui.motion.resolveSettingsIosPushForwardContentTransform
import com.android.purebilibili.core.ui.motion.resolveSettingsIosPushPopContentTransform
import com.android.purebilibili.navigation.resolveBottomPagerNavigationDurationMillis

private const val NAV3_FALLBACK_FADE_MILLIS = 180
private const val NAV3_REDUCED_MOTION_FADE_MILLIS = 140
private const val NAV3_SPACE_FORWARD_MILLIS = 220
private const val NAV3_LIGHT_SIBLING_MILLIS = 240
private val NAV3_BOTTOM_BAR_SIBLING_MILLIS =
    resolveBottomPagerNavigationDurationMillis(pageDistance = 1)
internal fun resolveBiliPaiNavContentTransform(
    routeTransition: BiliPaiNavRouteTransition
): ContentTransform {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT ->
            EnterTransition.None togetherWith ExitTransition.None
        BiliPaiNavRouteTransition.REDUCED_MOTION_FADE ->
            fadeIn(animationSpec = tween(NAV3_REDUCED_MOTION_FADE_MILLIS)) togetherWith
                fadeOut(animationSpec = tween(NAV3_REDUCED_MOTION_FADE_MILLIS))
        // LEFT/RIGHT 是兼容既有路由状态的历史分类名。实际页面转场由 Miuix
        // NavDisplay 按“全局导航动画”设置驱动；这里必须无动画，避免叠加第二层 Compose 转场。
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ->
            EnterTransition.None togetherWith ExitTransition.None
        BiliPaiNavRouteTransition.SPACE_FORWARD ->
            spaceForwardTransform()
        BiliPaiNavRouteTransition.LIGHT_SIBLING_FORWARD ->
            lightSiblingForwardTransform()
        BiliPaiNavRouteTransition.LIGHT_SIBLING_POP ->
            lightSiblingPopTransform()
        BiliPaiNavRouteTransition.BOTTOM_BAR_SIBLING_FORWARD ->
            bottomBarSiblingForwardTransform()
        BiliPaiNavRouteTransition.BOTTOM_BAR_SIBLING_POP ->
            bottomBarSiblingPopTransform()
        // 设置树 iOS push/pop：只横滑顶层设置页，底层页静止（见
        // resolveSettingsIosPushForwardContentTransform / resolveSettingsIosPushPopContentTransform）。
        BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_FORWARD ->
            settingsIosPushForwardTransform()
        BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP ->
            settingsIosPushPopTransform()
        BiliPaiNavRouteTransition.CLASSIC_CARD,
        BiliPaiNavRouteTransition.FALLBACK ->
            fadeIn(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS)) togetherWith
                fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
    }
}

/**
 * 设置页 push：只滑顶层设置页，底层页静止；固定时长 tween，不用 spring。
 */
private fun settingsIosPushForwardTransform(): ContentTransform =
    resolveSettingsIosPushForwardContentTransform(durationMillis = SETTINGS_IOS_PUSH_DURATION_MS)

/**
 * 设置页 pop：设置页向右滑出，底层页静止；固定时长 tween 承接预测手势 seek 收尾。
 */
private fun settingsIosPushPopTransform(): ContentTransform =
    resolveSettingsIosPushPopContentTransform(durationMillis = SETTINGS_IOS_PUSH_DURATION_MS)

private fun spaceForwardTransform(): ContentTransform {
    val spatialSpec = navigationSlideSpring(NAV3_SPACE_FORWARD_MILLIS)
    return (
        slideInHorizontally(
            animationSpec = spatialSpec,
            initialOffsetX = { width -> width / 8 }
        ) + fadeIn(animationSpec = tween(NAV3_SPACE_FORWARD_MILLIS))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
}

private fun lightSiblingForwardTransform(): ContentTransform {
    val spatialSpec = navigationSlideSpring(NAV3_LIGHT_SIBLING_MILLIS)
    return (
        slideInHorizontally(
            animationSpec = spatialSpec,
            initialOffsetX = { width -> width / 8 }
        ) + fadeIn(animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedEnter))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
}

private fun lightSiblingPopTransform(): ContentTransform {
    val spatialSpec = navigationSlideSpring(NAV3_LIGHT_SIBLING_MILLIS)
    return EnterTransition.None togetherWith
        (
            slideOutHorizontally(
                animationSpec = spatialSpec,
                targetOffsetX = { width -> width / 8 }
            ) + fadeOut(animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedExit))
        )
}

private fun bottomBarSiblingForwardTransform(): ContentTransform =
    resolveBottomBarLikeHorizontalContentTransform(
        durationMillis = NAV3_BOTTOM_BAR_SIBLING_MILLIS,
        forward = true
    )

private fun bottomBarSiblingPopTransform(): ContentTransform =
    resolveBottomBarLikeHorizontalContentTransform(
        durationMillis = NAV3_BOTTOM_BAR_SIBLING_MILLIS,
        forward = false
    )
