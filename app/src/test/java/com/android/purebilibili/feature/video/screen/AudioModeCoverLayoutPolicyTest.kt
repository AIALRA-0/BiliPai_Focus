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

    @Test
    fun verticalPageTransformTiltsAroundHorizontalAxis() {
        val style = resolveAudioModeCoverArtworkStyle()

        assertEquals(
            AudioModeVerticalPageTransform(
                rotationXDegrees = 9f,
                translationYDp = -17f,
                scale = 1f,
                alpha = 0.86f,
                pivotFractionY = 0f
            ),
            resolveAudioModeVerticalPageTransform(
                pageOffset = 0.5f,
                style = style
            )
        )
    }

    @Test
    fun verticalPageTransformClampsFarPages() {
        val style = resolveAudioModeCoverArtworkStyle()

        assertEquals(
            AudioModeVerticalPageTransform(
                rotationXDegrees = -18f,
                translationYDp = 34f,
                scale = 1f,
                alpha = 0.72f,
                pivotFractionY = 1f
            ),
            resolveAudioModeVerticalPageTransform(
                pageOffset = -2f,
                style = style
            )
        )
    }
}
