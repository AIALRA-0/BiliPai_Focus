package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortraitStoryNavigationPolicyTest {

    @Test
    fun resolveSeed_returnsPortraitStoryWhenDirectEntryEnabledForVerticalVideo() {
        val seed = resolvePortraitStoryNavigationSeed(
            directPortraitStoryEntry = true,
            isVerticalVideo = true,
            startAudio = false,
            bvid = "BV1portrait",
            cid = 66L,
            coverUrl = "https://img.test.com/portrait.jpg",
            cardTransitionEnabled = false,
        )

        assertEquals(
            PortraitStoryNavigationSeed(
                bvid = "BV1portrait",
                cid = 66L,
                coverUrl = "https://img.test.com/portrait.jpg"
            ),
            seed
        )
    }

    @Test
    fun resolveSeed_returnsNullWhenCardTransitionEnabled_useDetailMorphInstead() {
        val seed = resolvePortraitStoryNavigationSeed(
            directPortraitStoryEntry = true,
            isVerticalVideo = true,
            startAudio = false,
            bvid = "BV1portrait",
            cardTransitionEnabled = true,
        )

        assertNull(seed)
    }

    @Test
    fun resolveSeed_returnsNullWhenDirectEntryDisabled() {
        val seed = resolvePortraitStoryNavigationSeed(
            directPortraitStoryEntry = false,
            isVerticalVideo = true,
            startAudio = false,
            bvid = "BV1portrait"
        )

        assertNull(seed)
    }

    @Test
    fun resolveSeed_returnsNullForAudioPlayback() {
        val seed = resolvePortraitStoryNavigationSeed(
            directPortraitStoryEntry = true,
            isVerticalVideo = true,
            startAudio = true,
            bvid = "BV1audio"
        )

        assertNull(seed)
    }

    @Test
    fun resolveSeed_returnsNullForHorizontalVideo() {
        val seed = resolvePortraitStoryNavigationSeed(
            directPortraitStoryEntry = true,
            isVerticalVideo = false,
            startAudio = false,
            bvid = "BV1landscape"
        )

        assertNull(seed)
    }

    @Test
    fun detailMorphEntry_enabledOnlyWhenSettingAndCardTransitionAndVertical() {
        assertTrue(
            resolveDirectPortraitDetailMorphEntry(
                directPortraitStoryEntry = true,
                cardTransitionEnabled = true,
                isVerticalVideo = true,
            )
        )
        assertFalse(
            resolveDirectPortraitDetailMorphEntry(
                directPortraitStoryEntry = true,
                cardTransitionEnabled = false,
                isVerticalVideo = true,
            )
        )
        assertFalse(
            resolveDirectPortraitDetailMorphEntry(
                directPortraitStoryEntry = false,
                cardTransitionEnabled = true,
                isVerticalVideo = true,
            )
        )
        assertTrue(
            resolveDirectPortraitDetailMorphEntry(
                directPortraitStoryEntry = true,
                cardTransitionEnabled = true,
                isVerticalVideo = false,
                coverUrl = "https://img.test.com/pic@720w_1280h.jpg",
            )
        )
    }
}
