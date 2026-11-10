package com.android.purebilibili.feature.video.ui.feedback

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoActionFeedbackPolicyTest {

    @Test
    fun `inactive actions use inactive tint for icon`() {
        val inactiveTint = Color(0xFF8899AA)
        val activeTint = Color(0xFF3366FF)

        assertEquals(
            inactiveTint,
            resolveVideoActionTint(
                isActive = false,
                activeColor = activeTint,
                inactiveColor = inactiveTint
            )
        )
    }

    @Test
    fun `active actions always use shared theme tint for icon`() {
        val inactiveTint = Color(0xFF8899AA)
        val activeTint = Color(0xFF3366FF)

        assertEquals(
            activeTint,
            resolveVideoActionTint(
                isActive = true,
                activeColor = activeTint,
                inactiveColor = inactiveTint
            )
        )
    }

    @Test
    fun `inactive count tint stays muted`() {
        val inactiveTint = Color(0xFF8899AA)
        val activeTint = Color(0xFF3366FF)

        assertEquals(
            inactiveTint,
            resolveVideoActionCountTint(
                isActive = false,
                activeColor = activeTint,
                inactiveColor = inactiveTint
            )
        )
    }

    @Test
    fun `active count tint follows the shared theme family`() {
        val inactiveTint = Color(0xFF8899AA)
        val activeTint = Color(0xFF3366FF)

        assertEquals(
            activeTint,
            resolveVideoActionCountTint(
                isActive = true,
                activeColor = activeTint,
                inactiveColor = inactiveTint
            )
        )
    }

    @Test
    fun `detail action active colors derive from active theme roles`() {
        val primary = Color(0xFF3366FF)
        val secondary = Color(0xFF00A884)

        val colors = resolveVideoDetailActionActiveColors(
            primary = primary,
            secondary = secondary
        )

        assertEquals(primary, colors.primaryAction)
        assertEquals(primary, colors.watchLater)
        assertEquals(secondary, colors.downloadInProgress)
        assertEquals(primary, colors.downloaded)
    }

    @Test
    fun `miuix active actions do not inherit neutral secondary color`() {
        val primary = Color(0xFF3366FF)
        val colors = resolveVideoDetailActionActiveColors(
            primary = primary,
            secondary = Color.Gray,
            uiStyle = AppUiStyle.MIUIX,
        )

        assertEquals(primary, colors.watchLater)
        assertEquals(primary, colors.downloadInProgress)
        assertEquals(primary, colors.downloaded)
    }

    @Test
    fun `portrait placement anchors feedback at bottom center`() {
        val placement = resolveVideoFeedbackPlacement(
            isFullscreen = false,
            isLandscape = false,
            bottomInsetDp = 18
        )

        assertEquals(VideoFeedbackAnchor.BottomCenter, placement.anchor)
        assertEquals(18, placement.bottomInsetDp)
        assertEquals(0, placement.sideInsetDp)
    }

    @Test
    fun `fullscreen landscape placement uses bottom trailing safe area`() {
        val placement = resolveVideoFeedbackPlacement(
            isFullscreen = true,
            isLandscape = true,
            bottomInsetDp = 24
        )

        assertEquals(VideoFeedbackAnchor.BottomTrailing, placement.anchor)
        assertEquals(24, placement.bottomInsetDp)
        assertEquals(24, placement.sideInsetDp)
    }

    @Test
    fun `quality reminder placement uses centered emphasized overlay`() {
        val placement = resolveQualityReminderPlacement()

        assertEquals(VideoFeedbackAnchor.CenterOverlay, placement.anchor)
        assertEquals(VideoFeedbackEmphasis.Emphasized, placement.emphasis)
    }

    @Test
    fun `like feedback clears controls and the navigation inset`() {
        val placement = resolveLikeBurstPlacement(bottomInsetDp = 24)

        assertEquals(VideoFeedbackAnchor.BottomTrailing, placement.anchor)
        assertEquals(88, placement.bottomInsetDp)
        assertEquals(16, placement.sideInsetDp)
        assertEquals(VideoFeedbackEmphasis.Emphasized, placement.emphasis)
    }
}
