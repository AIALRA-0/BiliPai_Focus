package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioModeCoverLayoutPolicyTest {

    @Test
    fun usesWidthFractionWhenContentAreaIsTallEnough() {
        assertEquals(
            362,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 420
            )
        )
    }

    @Test
    fun shrinksCoverWhenContentAreaNeedsTopAndBottomClearance() {
        assertEquals(
            284,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 300
            )
        )
    }

    @Test
    fun neverReturnsNegativeCoverSizeOnVeryShortLayouts() {
        assertEquals(
            24,
            resolveAudioModeCenteredCoverSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 40
            )
        )
    }

    @Test
    fun artworkStyleUsesAppleMusicLikeRoundedShadowedCover() {
        val style = resolveAudioModeCoverArtworkStyle()

        assertEquals(26, style.cornerRadiusDp)
        assertEquals(28, style.shadowElevationDp)
        assertEquals(46, style.backgroundScrimAlphaPercent)
        assertEquals(16, style.aspectWidth)
        assertEquals(10, style.aspectHeight)
        assertEquals(18, style.maxRotationDegrees)
        assertEquals(34, style.maxTranslationDp)
        assertEquals(10, style.maxScaleLossPercent)
        assertEquals(28, style.maxAlphaLossPercent)
    }

    @Test
    fun artworkSizeUsesWideRoundedRectangle() {
        assertEquals(
            AudioModeArtworkSizeDp(widthDp = 362, heightDp = 226),
            resolveAudioModeArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 420
            )
        )
        assertEquals(
            AudioModeArtworkSizeDp(widthDp = 362, heightDp = 226),
            resolveAudioModeArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 300
            )
        )
    }
}
