package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerOverlayLayoutPolicyTest {

    @Test
    fun compactPhone_usesCurrentMiniPlayerSize() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 393
        )

        assertEquals(220, policy.miniPlayerWidthDp)
        assertEquals(130, policy.miniPlayerHeightDp)
        assertEquals(28, policy.headerHeightDp)
        assertEquals(24, policy.headerButtonSizeDp)
        assertEquals(8, policy.seekHintCornerRadiusDp)
        assertEquals(8, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun mediumTablet_keepsReadableMiniPlayerWithoutOverflow() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 720
        )

        assertEquals(280, policy.miniPlayerWidthDp)
        assertEquals(157, policy.miniPlayerHeightDp)
        assertEquals(28, policy.headerHeightDp)
        assertEquals(24, policy.headerButtonSizeDp)
        assertEquals(8, policy.seekHintCornerRadiusDp)
        assertEquals(8, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun expandedTablet_scalesMiniPlayerAndControls() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 1280
        )

        assertEquals(300, policy.miniPlayerWidthDp)
        assertEquals(168, policy.miniPlayerHeightDp)
        assertEquals(30, policy.headerHeightDp)
        assertEquals(26, policy.headerButtonSizeDp)
        assertEquals(10, policy.seekHintCornerRadiusDp)
        assertEquals(10, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun ultraWide_usesLargestTenFootMiniPlayerTier() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(320, policy.miniPlayerWidthDp)
        assertEquals(180, policy.miniPlayerHeightDp)
        assertEquals(32, policy.headerHeightDp)
        assertEquals(28, policy.headerButtonSizeDp)
        assertEquals(12, policy.seekHintCornerRadiusDp)
        assertEquals(12, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun adaptiveDimensions_horizontalVideo_anchorsWidthAndComputesHeight() {
        val dimensions = resolveAdaptiveMiniPlayerDimensions(
            videoAspectRatio = 16f / 9f,
            currentWidthDp = 220f,
            defaultHeightDp = 130f
        )

        assertEquals(220f, dimensions.widthDp)
        assertEquals(220f / (16f / 9f), dimensions.heightDp, 0.01f)
        assertEquals(16f / 9f, dimensions.aspectRatio, 0.001f)
    }

    @Test
    fun adaptiveDimensions_verticalVideo_zeroBlackBars_anchorsHeightAndComputesWidth() {
        val dimensions = resolveAdaptiveMiniPlayerDimensions(
            videoAspectRatio = 9f / 16f,
            currentWidthDp = 220f,
            defaultHeightDp = 130f
        )

        val expectedHeight = 130f * 1.55f // 201.5f
        val expectedWidth = expectedHeight * (9f / 16f) // 113.34375f

        assertEquals(expectedWidth, dimensions.widthDp, 0.01f)
        assertEquals(expectedHeight, dimensions.heightDp, 0.01f)
        assertEquals(9f / 16f, dimensions.aspectRatio, 0.001f)
    }

    @Test
    fun adaptiveDimensions_fallbackWhenZeroOrNegativeAspect() {
        val dimensions = resolveAdaptiveMiniPlayerDimensions(
            videoAspectRatio = 0f,
            currentWidthDp = 220f,
            defaultHeightDp = 130f
        )

        assertEquals(16f / 9f, dimensions.aspectRatio, 0.001f)
        assertEquals(220f, dimensions.widthDp)
    }
}

