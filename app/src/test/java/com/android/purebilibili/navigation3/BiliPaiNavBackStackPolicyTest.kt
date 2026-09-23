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
    fun initialBackStack_opensPortraitFeedOnStartupWhenEnabled() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.Story()),
            resolveInitialBiliPaiBackStack(
                firstRoute = ScreenRoutes.Home.route,
                onboardingRequired = false,
                openPortraitFeedOnStartup = true
            )
        )
    }

    @Test
    fun push_skipsDuplicateTopEntry() {
        val stack = listOf(BiliPaiNavKey.MainHost)

        assertEquals(stack, pushBiliPaiNavKey(stack, BiliPaiNavKey.MainHost))
    }

    @Test
    fun push_existingSearchPopsToItsUniqueBackStackInstance() {
        val stack = listOf(
            BiliPaiNavKey.MainHost,
            BiliPaiNavKey.Search,
            BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "search", openId = 1L),
        )

        assertEquals(
            listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.Search),
            pushBiliPaiNavKey(stack, BiliPaiNavKey.Search),
        )
    }

    @Test
    fun push_sameUpSpaceIgnoresTransientTargetVideoAndPopsVideoDetail() {
        val originalSpace = BiliPaiNavKey.Space(mid = 42L)
        val video = BiliPaiNavKey.VideoDetail(
            bvid = "BV1",
            sourceRoute = originalSpace.toLegacyRoute(),
            openId = 1L,
        )

        assertEquals(
            listOf(BiliPaiNavKey.MainHost, originalSpace),
            pushBiliPaiNavKey(
                currentStack = listOf(BiliPaiNavKey.MainHost, originalSpace, video),
                key = BiliPaiNavKey.Space(mid = 42L, targetBvid = "BV1"),
            ),
        )
    }

    @Test
    fun push_differentUpSpaceStillAppends() {
        val originalSpace = BiliPaiNavKey.Space(mid = 42L)
        val video = BiliPaiNavKey.VideoDetail("BV1", openId = 1L)
        val otherSpace = BiliPaiNavKey.Space(mid = 84L, targetBvid = "BV1")

        assertEquals(
            listOf(BiliPaiNavKey.MainHost, originalSpace, video, otherSpace),
            pushBiliPaiNavKey(
                currentStack = listOf(BiliPaiNavKey.MainHost, originalSpace, video),
                key = otherSpace,
            ),
        )
    }

    @Test
    fun push_distinctInstanceKeyStillAppends() {
        val first = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "search", openId = 1L)
        val second = first.copy(openId = 2L)

        assertEquals(
            listOf(BiliPaiNavKey.MainHost, first, second),
            pushBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost, first), second),
        )
    }

    @Test
    fun push_liveAreaDetailReentryKeepsDistinctInstances() {
        // 回归：同一直播分区经同级 chips 互跳后再次进入时，openId 使每次 push 的
        // contentKey 实例唯一，避免 Miuix 抛出 Duplicate contentKey 崩溃。
        val first = BiliPaiNavKey.LiveAreaDetail(parentAreaId = 1, areaId = 145, title = "颜值", openId = 100L)
        val reentry = BiliPaiNavKey.LiveAreaDetail(parentAreaId = 1, areaId = 145, title = "颜值", openId = 101L)

        val stack = pushBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost, first), reentry)

        assertEquals(listOf(BiliPaiNavKey.MainHost, first, reentry), stack)
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
    fun pushOrReplaceSettingsCategory_replacesSiblingCategory() {
        val a = BiliPaiNavKey.SettingsCategory(com.android.purebilibili.feature.settings.SettingsRootCategory.APPEARANCE_INTERACTION)
        val b = BiliPaiNavKey.SettingsCategory(com.android.purebilibili.feature.settings.SettingsRootCategory.CONTENT_PLAYBACK)
        assertEquals(
            listOf(BiliPaiNavKey.MainHost, b),
            pushOrReplaceSettingsCategoryNavKey(listOf(BiliPaiNavKey.MainHost, a), b)
        )
    }

    @Test
    fun pushOrReplaceSettingsCategory_replacesDetailChainOnTabletSwitch() {
        val category = BiliPaiNavKey.SettingsCategory(
            com.android.purebilibili.feature.settings.SettingsRootCategory.CONTENT_PLAYBACK
        )
        assertEquals(
            listOf(BiliPaiNavKey.MainHost, category),
            pushOrReplaceSettingsCategoryNavKey(
                listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.AppearanceSettings),
                category,
            )
        )
        assertEquals(
            listOf(BiliPaiNavKey.MainHost, category),
            pushOrReplaceSettingsCategoryNavKey(
                listOf(
                    BiliPaiNavKey.MainHost,
                    BiliPaiNavKey.SettingsCategory(
                        com.android.purebilibili.feature.settings.SettingsRootCategory.APPEARANCE_INTERACTION
                    ),
                    BiliPaiNavKey.AppearanceSettings,
                ),
                category,
            )
        )
    }

    @Test
    fun pushOrReplaceSettingsCategory_pushesOnSettingsRoot() {
        val category = BiliPaiNavKey.SettingsCategory(
            com.android.purebilibili.feature.settings.SettingsRootCategory.SYSTEM_ABOUT
        )
        assertEquals(
            listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.Settings, category),
            pushOrReplaceSettingsCategoryNavKey(
                listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.Settings),
                category,
            )
        )
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

        assertTrue(onboardingFinishBlock.contains("USER_AGREEMENT_ACK_KEY"))
        assertTrue(onboardingFinishBlock.contains("onDisagree"))
        assertTrue(onboardingFinishBlock.contains("finishAffinity()"))
        assertTrue(onboardingFinishBlock.contains("resolveInitialBiliPaiBackStack("))
        assertFalse(onboardingFinishBlock.contains("applyOnboardingSettingsGuidePreset("))
        assertFalse(onboardingFinishBlock.contains("navigation3BackStack = listOf(BiliPaiNavKey.Home)"))
    }
}
