package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoDetailPlayerCollapsePolicyTest {

    @Test
    fun `resolveNextPlayerHeightOffset ignores tiny deltas`() {
        val next = resolveNextPlayerHeightOffset(
            currentOffsetPx = -24f,
            deltaPx = 0.2f,
            minOffsetPx = -360f
        )

        assertNull(next)
    }

    @Test
    fun `resolveNextPlayerHeightOffset clamps into valid range`() {
        assertEquals(
            -360f,
            resolveNextPlayerHeightOffset(
                currentOffsetPx = -300f,
                deltaPx = -120f,
                minOffsetPx = -360f
            )
        )

        assertEquals(
            0f,
            resolveNextPlayerHeightOffset(
                currentOffsetPx = -10f,
                deltaPx = 30f,
                minOffsetPx = -360f
            )
        )
    }

    @Test
    fun `resolveNextPlayerHeightOffset returns null when clamped result unchanged`() {
        val next = resolveNextPlayerHeightOffset(
            currentOffsetPx = 0f,
            deltaPx = 32f,
            minOffsetPx = -360f
        )

        assertNull(next)
    }

    @Test
    fun `resolveIsPlayerCollapsed respects setting switch`() {
        assertFalse(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = false,
                playerHeightOffsetPx = -600f,
                videoHeightPx = 500f
            )
        )
    }

    @Test
    fun `resolveIsPlayerCollapsed enters collapsed state near lower bound`() {
        assertTrue(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = true,
                playerHeightOffsetPx = -495f,
                videoHeightPx = 500f,
                collapseTolerancePx = 10f
            )
        )

        assertFalse(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = true,
                playerHeightOffsetPx = -460f,
                videoHeightPx = 500f,
                collapseTolerancePx = 10f
            )
        )
    }

    @Test
    fun `inline layout resets for portrait external navigation only when portrait fullscreen is active`() {
        assertTrue(
            shouldResetInlineLayoutForPortraitExternalNavigation(
                isPortraitFullscreen = true
            )
        )
        assertFalse(
            shouldResetInlineLayoutForPortraitExternalNavigation(
                isPortraitFullscreen = false
            )
        )
    }

    @Test
    fun `inline layout resets on deferred portrait return only after portrait fullscreen exits`() {
        assertTrue(
            shouldResetInlineLayoutOnDeferredPortraitReturn(
                hasDeferredRestore = true,
                isPortraitFullscreen = false
            )
        )
        assertFalse(
            shouldResetInlineLayoutOnDeferredPortraitReturn(
                hasDeferredRestore = false,
                isPortraitFullscreen = false
            )
        )
        assertFalse(
            shouldResetInlineLayoutOnDeferredPortraitReturn(
                hasDeferredRestore = true,
                isPortraitFullscreen = true
            )
        )
    }
}
