package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.store.HomeCardBadgeEffectMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeVideoGlassBadgeStylePolicyTest {

    @Test
    fun softGlass_usesGlassStyleWithoutRealtimeHaze() {
        val visual = resolveHomeCardBadgeEffectVisual(
            mode = HomeCardBadgeEffectMode.SOFT_GLASS,
            scrollLiteModeEnabled = false,
            hasHazeState = true
        )
        assertEquals(HomeVideoBadgeStyle.GLASS, visual.coverStyle)
        assertTrue(visual.glassEnabled)
        assertFalse(visual.blurEnabled)
        assertFalse(visual.useRealtimeHaze)
        assertEquals(HomeCardBadgeEffectMode.SOFT_GLASS, visual.effectiveMode)
    }

    @Test
    fun lightBlur_keepsRealtimeHazeWhileScrolling_whenSourceAvailable() {
        val idle = resolveHomeCardBadgeEffectVisual(
            mode = HomeCardBadgeEffectMode.LIGHT_BLUR,
            scrollLiteModeEnabled = false,
            hasHazeState = true
        )
        val scrolling = resolveHomeCardBadgeEffectVisual(
            mode = HomeCardBadgeEffectMode.LIGHT_BLUR,
            scrollLiteModeEnabled = true,
            hasHazeState = true
        )
        assertTrue(idle.useRealtimeHaze)
        assertTrue(idle.blurEnabled)
        assertEquals(HomeCardBadgeEffectMode.LIGHT_BLUR, idle.effectiveMode)
        // Match bottom bar: do not demote to soft glass while scrolling.
        assertTrue(scrolling.useRealtimeHaze)
        assertTrue(scrolling.blurEnabled)
        assertEquals(HomeCardBadgeEffectMode.LIGHT_BLUR, scrolling.effectiveMode)
    }

    @Test
    fun lightBlur_withoutHazeFallsBackToFrostedSoftShell() {
        val visual = resolveHomeCardBadgeEffectVisual(
            mode = HomeCardBadgeEffectMode.LIGHT_BLUR,
            scrollLiteModeEnabled = false,
            hasHazeState = false
        )
        assertTrue(visual.glassEnabled)
        assertTrue(visual.blurEnabled)
        assertFalse(visual.useRealtimeHaze)
        assertEquals(HomeCardBadgeEffectMode.LIGHT_BLUR, visual.effectiveMode)
    }

    @Test
    fun off_usesPlainBadges() {
        val visual = resolveHomeCardBadgeEffectVisual(
            mode = HomeCardBadgeEffectMode.OFF,
            scrollLiteModeEnabled = false,
            hasHazeState = true
        )
        assertEquals(HomeVideoBadgeStyle.PLAIN, visual.coverStyle)
        assertFalse(visual.glassEnabled)
        assertFalse(visual.useRealtimeHaze)
    }

    @Test
    fun legacyBooleanPolicy_mapsToSoftGlassOrOff() {
        val on = resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = true,
            showInfoGlassBadges = true
        )
        val off = resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = false,
            showInfoGlassBadges = false
        )
        assertEquals(HomeVideoBadgeStyle.GLASS, on.coverStyle)
        assertEquals(HomeVideoBadgeStyle.PLAIN, off.coverStyle)
    }
}
