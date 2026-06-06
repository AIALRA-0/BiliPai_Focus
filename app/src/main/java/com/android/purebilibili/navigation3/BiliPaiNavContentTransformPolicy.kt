package com.android.purebilibili.navigation3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.android.purebilibili.core.ui.motion.AppMotionEasing

private const val NAV3_FALLBACK_FADE_MILLIS = 180
private const val NAV3_DISABLED_VIDEO_DIRECTION_MILLIS = 220
private const val NAV3_DISABLED_VIDEO_RETURN_MILLIS = 320
private const val NAV3_SPACE_FORWARD_MILLIS = 220
private const val NAV3_LIGHT_SIBLING_MILLIS = 220
internal fun resolveBiliPaiNavContentTransform(
    routeTransition: BiliPaiNavRouteTransition
): ContentTransform {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT ->
            EnterTransition.None togetherWith ExitTransition.None
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT ->
            disabledVideoDirectionForwardTransform(directionSign = -1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT ->
            disabledVideoDirectionForwardTransform(directionSign = 1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT ->
            disabledVideoDirectionReturnTransform(directionSign = -1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ->
            disabledVideoDirectionReturnTransform(directionSign = 1)
        BiliPaiNavRouteTransition.SPACE_FORWARD ->
            spaceForwardTransform()
        BiliPaiNavRouteTransition.LIGHT_SIBLING_FORWARD ->
            lightSiblingForwardTransform()
        BiliPaiNavRouteTransition.LIGHT_SIBLING_POP ->
            lightSiblingPopTransform()
        BiliPaiNavRouteTransition.CLASSIC_CARD,
        BiliPaiNavRouteTransition.FALLBACK ->
            fadeIn(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS)) togetherWith
                fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
    }
}

internal fun resolveBiliPaiNavPopContentTransform(
    routeTransition: BiliPaiNavRouteTransition
): ContentTransform? {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ->
            resolveBiliPaiNavContentTransform(routeTransition)
        else -> null
    }
}

private fun disabledVideoDirectionForwardTransform(directionSign: Int): ContentTransform {
    return (
        slideInHorizontally(
            animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS, easing = AppMotionEasing.EmphasizedEnter),
            initialOffsetX = { width -> directionSign * width / 4 }
        ) + fadeIn(animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS, easing = AppMotionEasing.EmphasizedEnter))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS))
}

private fun spaceForwardTransform(): ContentTransform {
    return (
        slideInHorizontally(
            animationSpec = tween(NAV3_SPACE_FORWARD_MILLIS, easing = AppMotionEasing.EmphasizedEnter),
            initialOffsetX = { width -> width / 8 }
        ) + fadeIn(animationSpec = tween(NAV3_SPACE_FORWARD_MILLIS))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
}

private fun lightSiblingForwardTransform(): ContentTransform {
    return (
        slideInHorizontally(
            animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedEnter),
            initialOffsetX = { width -> width / 8 }
        ) + fadeIn(animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedEnter))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
}

private fun lightSiblingPopTransform(): ContentTransform {
    return EnterTransition.None togetherWith
        (
            slideOutHorizontally(
                animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedExit),
                targetOffsetX = { width -> width / 8 }
            ) + fadeOut(animationSpec = tween(NAV3_LIGHT_SIBLING_MILLIS, easing = AppMotionEasing.EmphasizedExit))
        )
}

private fun disabledVideoDirectionReturnTransform(directionSign: Int): ContentTransform {
    return EnterTransition.None togetherWith
        (
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = NAV3_DISABLED_VIDEO_RETURN_MILLIS,
                    easing = FastOutSlowInEasing
                ),
                targetOffsetX = { width -> directionSign * width / 2 }
            ) + fadeOut(
                animationSpec = tween(NAV3_DISABLED_VIDEO_RETURN_MILLIS),
                targetAlpha = 0.90f
            )
        )
}
