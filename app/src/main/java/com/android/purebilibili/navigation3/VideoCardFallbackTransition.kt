package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

internal fun resolveVideoCardFallbackTransition(
    cardTransitionEnabled: Boolean,
    animationStyle: BiliPaiPredictiveBackAnimationStyle,
    globalTransition: NavTransition,
    cardEnabledFallback: NavTransition,
): NavTransition = when {
    cardTransitionEnabled -> cardEnabledFallback
    animationStyle == BiliPaiPredictiveBackAnimationStyle.MIUIX -> NavTransitions.MiuixDefault
    else -> globalTransition
}
