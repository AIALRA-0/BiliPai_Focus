package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardScrollLiteVisualPolicyTest {

    @Test
    fun `normal mode removes cover gradient behind compact stats`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode keeps cover gradient when stats move below cover`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps stats without cover shadow`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertFalse(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps secondary row when cover stats are disabled`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode keeps story card secondary stats row`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode removes story card shadows but keeps stats`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `home video metadata uses on surface colors for readable up and publish text`() {
        val onSurface = Color(0xFF1D1B20)
        val colors = resolveHomeVideoCardMetadataColors(onSurface)

        assertEquals(onSurface, colors.upNameColor)
        assertEquals(onSurface.copy(alpha = 0.82f), colors.upMetaColor)
        assertEquals(onSurface.copy(alpha = 0.68f), colors.upBadgeTextColor)
        assertEquals(onSurface.copy(alpha = 0.10f), colors.upBadgeBackgroundColor)
        assertEquals(onSurface.copy(alpha = 0.72f), colors.publishTimeColor)
    }
}
