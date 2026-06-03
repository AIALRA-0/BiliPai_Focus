package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.ScreenRoutes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavBackStackPolicyTest {

    @Test
    fun initialBackStack_usesOnboardingWhenRequired() {
        assertEquals(
            listOf(BiliPaiNavKey.Onboarding),
            resolveInitialBiliPaiBackStack(
                firstRoute = ScreenRoutes.Home.route,
                onboardingRequired = true
            )
        )
    }

    @Test
    fun initialBackStack_usesMainHostForMainApp() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            resolveInitialBiliPaiBackStack(
                firstRoute = ScreenRoutes.Profile.route,
                onboardingRequired = false
            )
        )
    }

    @Test
    fun push_skipsDuplicateTopEntry() {
        val stack = listOf(BiliPaiNavKey.MainHost)

        assertEquals(stack, pushBiliPaiNavKey(stack, BiliPaiNavKey.MainHost))
    }

    @Test
    fun pop_keepsRootEntry() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost))
        )
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.VideoDetail("BV1")))
        )
    }

    @Test
    fun popToRoot_dropsAllEntriesAboveMainHost() {
        // 「返回首页」按钮：[MainHost, Search, VideoDetail] → [MainHost]，
        // 让 popTransitionSpec 一次性播放横向过渡。
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKeyToRoot(
                listOf(
                    BiliPaiNavKey.MainHost,
                    BiliPaiNavKey.Search,
                    BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "search")
                )
            )
        )
    }

    @Test
    fun popToRoot_isIdempotentAtMainHost() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKeyToRoot(listOf(BiliPaiNavKey.MainHost))
        )
    }

    @Test
    fun popToRoot_preservesNonMainHostRoot() {
        // Onboarding 流程或异常态：栈底不是 MainHost 时不应误删。
        val stack = listOf(BiliPaiNavKey.Onboarding, BiliPaiNavKey.VideoDetail("BV1"))

        assertEquals(stack, popBiliPaiNavKeyToRoot(stack))
    }

    @Test
    fun popToRoot_handlesEmptyStack() {
        assertEquals(emptyList(), popBiliPaiNavKeyToRoot(emptyList()))
    }

    @Test
    fun onboardingFinishEntersMainHostInsteadOfDirectHomeRoute() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }
        val source = sourceFile.readText()
        val onboardingFinishBlock = source
            .substringAfter("BiliPaiNavEntryContentRole.ONBOARDING")
            .substringBefore("BiliPaiNavEntryContentRole.SETTINGS")

        assertTrue(onboardingFinishBlock.contains("onApplySettingsProfile"))
        assertTrue(onboardingFinishBlock.contains("applyOnboardingSettingsGuidePreset("))
        assertTrue(onboardingFinishBlock.contains("navigation3BackStack = listOf(BiliPaiNavKey.MainHost)"))
        assertFalse(onboardingFinishBlock.contains("navigation3BackStack = listOf(BiliPaiNavKey.Home)"))
    }
}
