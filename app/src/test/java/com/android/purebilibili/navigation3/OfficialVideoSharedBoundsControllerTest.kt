package com.android.purebilibili.navigation3

import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfficialVideoSharedBoundsControllerTest {
    private fun session(bounds: Rect? = Rect(12f, 24f, 220f, 140f)) =
        VideoCardTransitionSession(
            bvid = "BV1",
            sourceRoute = "home",
            sourceKey = "home:BV1",
            cardBounds = bounds,
            coverBounds = null,
            sourceCornerDp = 28,
            cardSourceDirection = BiliPaiNavCardSourceDirection.NONE,
            coverIdentity = null,
            cardFullyVisible = true,
            isSingleColumnCard = false,
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
            sourceChromeSnapshot = null,
        )

    @Test
    fun openThenPredictiveCancelRestoresDetailWithoutPoppingTheSession() {
        val controller = OfficialVideoSharedBoundsController()
        controller.beginOpening(session())
        assertEquals(OfficialVideoSharedBoundsController.Phase.Opening, controller.phase)
        controller.onNavigationFrame(1f, VideoCardTransitionSettleState.Held, false)
        assertNull(controller.phase)

        controller.onNavigationFrame(.4f, VideoCardTransitionSettleState.InteractiveSeek, true)
        assertEquals(OfficialVideoSharedBoundsController.Phase.Returning, controller.phase)
        assertEquals(.4f, controller.progress)
        controller.onNavigationFrame(1f, VideoCardTransitionSettleState.Held, false)
        assertNull(controller.phase)
        assertEquals("BV1", controller.session?.bvid)
    }

    @Test
    fun committedReturnReleasesTheFrozenSource() {
        val controller = OfficialVideoSharedBoundsController()
        controller.beginOpening(session())
        controller.onNavigationFrame(1f, VideoCardTransitionSettleState.Held, false)
        controller.beginReturning()
        controller.onNavigationFrame(0f, VideoCardTransitionSettleState.Idle, false)
        assertNull(controller.session)
        assertNull(controller.phase)
    }

    @Test
    fun backDuringOpeningReversesTheSameBoundsTimeline() {
        val controller = OfficialVideoSharedBoundsController()
        controller.beginOpening(session())
        controller.onNavigationFrame(.5f, VideoCardTransitionSettleState.AutoEnter, false)
        controller.onNavigationFrame(.2f, VideoCardTransitionSettleState.InteractiveSeek, true)
        assertEquals(OfficialVideoSharedBoundsController.Phase.Opening, controller.phase)
        assertEquals(.2f, controller.progress)
        controller.onNavigationFrame(0f, VideoCardTransitionSettleState.Idle, false)
        assertNull(controller.session)
    }

    @Test
    fun missingSourceBoundsCannotStartASharedMorph() {
        val controller = OfficialVideoSharedBoundsController()
        controller.beginOpening(session(null))
        assertNull(controller.session)
        assertNull(controller.phase)
    }

    @Test
    fun nestedDetailReturnRestoresTheParentCardSource() {
        val controller = OfficialVideoSharedBoundsController()
        controller.beginOpening(session())
        controller.onNavigationFrame(1f, VideoCardTransitionSettleState.Held, false)
        controller.beginOpening(
            session().copy(bvid = "BV2", sourceKey = "video/BV1:BV2"),
        )
        controller.onNavigationFrame(1f, VideoCardTransitionSettleState.Held, false)
        controller.beginReturning()
        controller.onNavigationFrame(0f, VideoCardTransitionSettleState.Idle, false)

        assertEquals("BV1", controller.session?.bvid)
        assertNull(controller.phase)
    }
}
