package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenPolicyTest {

    @Test
    fun `settings back target chooses the topmost overlay`() {
        assertEquals(
            SettingsBackTarget.BLOCKED_LIST,
            resolveSettingsBackTarget(
                showBlockedList = true,
                showCacheDialog = true,
                showCacheAnimation = true,
            )
        )
        assertEquals(
            SettingsBackTarget.CACHE_ANIMATION,
            resolveSettingsBackTarget(
                showCacheDialog = true,
                showCacheAnimation = true,
            )
        )
    }

    @Test
    fun `settings back target is none without a local overlay`() {
        assertEquals(SettingsBackTarget.NONE, resolveSettingsBackTarget())
    }

    @Test
    fun settingsScroll_hidesBottomBarWhenBrowsingForward() {
        assertEquals(
            false,
            reduceSettingsBottomBarScroll(
                tracker = SettingsBottomBarScrollTracker(SettingsBottomBarScrollState(0, 120)),
                currentState = SettingsBottomBarScrollState(0, 200),
                topRevealThresholdPx = 24,
                directionThresholdPx = 32,
            ).bottomBarVisible,
        )
        assertEquals(
            false,
            reduceSettingsBottomBarScroll(
                tracker = SettingsBottomBarScrollTracker(SettingsBottomBarScrollState(0, 200)),
                currentState = SettingsBottomBarScrollState(1, 0),
                topRevealThresholdPx = 24,
                directionThresholdPx = 32,
            ).bottomBarVisible,
        )
    }

    @Test
    fun settingsScroll_showsBottomBarOnReverseScrollAndAtTop() {
        assertEquals(
            true,
            reduceSettingsBottomBarScroll(
                tracker = SettingsBottomBarScrollTracker(SettingsBottomBarScrollState(2, 120)),
                currentState = SettingsBottomBarScrollState(2, 60),
                topRevealThresholdPx = 24,
                directionThresholdPx = 32,
            ).bottomBarVisible,
        )
        assertEquals(
            true,
            reduceSettingsBottomBarScroll(
                tracker = SettingsBottomBarScrollTracker(SettingsBottomBarScrollState(1, 0)),
                currentState = SettingsBottomBarScrollState(0, 12),
                topRevealThresholdPx = 24,
                directionThresholdPx = 32,
            ).bottomBarVisible,
        )
    }

    @Test
    fun settingsScroll_accumulatesSlowMovementAndResetsOnDirectionChange() {
        val firstUpdate = reduceSettingsBottomBarScroll(
            tracker = SettingsBottomBarScrollTracker(SettingsBottomBarScrollState(1, 100)),
            currentState = SettingsBottomBarScrollState(1, 120),
            topRevealThresholdPx = 24,
            directionThresholdPx = 32,
        )
        assertEquals(null, firstUpdate.bottomBarVisible)

        val secondUpdate = reduceSettingsBottomBarScroll(
            tracker = firstUpdate.tracker,
            currentState = SettingsBottomBarScrollState(1, 136),
            topRevealThresholdPx = 24,
            directionThresholdPx = 32,
        )
        assertEquals(false, secondUpdate.bottomBarVisible)

        val reverseJitter = reduceSettingsBottomBarScroll(
            tracker = secondUpdate.tracker,
            currentState = SettingsBottomBarScrollState(1, 128),
            topRevealThresholdPx = 24,
            directionThresholdPx = 32,
        )
        assertEquals(null, reverseJitter.bottomBarVisible)
    }

    @Test
    fun topLevelSettings_bottomPaddingIncludesVisibleBottomBarHeight() {
        val padding = resolveSettingsContentBottomPadding(
            navigationBarsBottom = 16.dp,
            bottomBarVisible = true,
            isBottomBarFloating = true,
            bottomBarLabelMode = 0,
            isTablet = false
        )

        assertEquals(142.dp, padding)
    }

    @Test
    fun secondarySettingsLayout_keepsLegacyBottomPaddingWhenBottomBarHidden() {
        val padding = resolveSettingsContentBottomPadding(
            navigationBarsBottom = 16.dp,
            bottomBarVisible = false,
            isBottomBarFloating = true,
            bottomBarLabelMode = 0,
            isTablet = false
        )

        assertEquals(44.dp, padding)
    }
}
