package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import org.junit.Assert.assertSame
import org.junit.Test
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

class VideoCardFallbackTransitionTest {
    @Test
    fun disabledCardDefaultBypassesGlobalProgressAndBlurWrappers() {
        assertSame(
            NavTransitions.MiuixDefault,
            resolveVideoCardFallbackTransition(
                cardTransitionEnabled = false,
                animationStyle = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                globalTransition = NavTransitions.Modal,
                cardEnabledFallback = NavTransitions.None,
            ),
        )
    }

    @Test
    fun disabledCardPreservesExplicitStylesAndReducedMotion() {
        BiliPaiPredictiveBackAnimationStyle.entries
            .filter { it != BiliPaiPredictiveBackAnimationStyle.MIUIX }
            .forEach { style ->
                val selected = if (style == BiliPaiPredictiveBackAnimationStyle.NONE) {
                    NavTransitions.None
                } else {
                    NavTransitions.Modal
                }
                assertSame(
                    selected,
                    resolveVideoCardFallbackTransition(
                        cardTransitionEnabled = false,
                        animationStyle = style,
                        globalTransition = selected,
                        cardEnabledFallback = NavTransitions.MiuixDefault,
                    ),
                )
            }
    }

    @Test
    fun enabledCardRetainsItsOwnFallbackForEveryGlobalStyle() {
        BiliPaiPredictiveBackAnimationStyle.entries.forEach { style ->
            assertSame(
                NavTransitions.Modal,
                resolveVideoCardFallbackTransition(
                    cardTransitionEnabled = true,
                    animationStyle = style,
                    globalTransition = NavTransitions.None,
                    cardEnabledFallback = NavTransitions.Modal,
                ),
            )
        }
    }
}
