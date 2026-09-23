package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailReturnPolicyTest {

    @Test
    fun quickReturn_whenBackWithinThreshold_returnsTrue() {
        assertTrue(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_420L
            )
        )
    }

    @Test
    fun slowReturn_whenBackAfterThreshold_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_650L
            )
        )
    }

    @Test
    fun invalidTimeline_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 2_000L,
                detailReturnUptimeMs = 1_900L
            )
        )
    }

    @Test
    fun clear_resetsOnlyCardGeometryFallback() {
        CardPositionManager.recordCardPosition(
            bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
            screenWidth = 200f,
            screenHeight = 200f
        )
        assertTrue(CardPositionManager.lastClickedCardBounds != null)
        assertTrue(CardPositionManager.lastClickedCardCenter != null)

        CardPositionManager.clear()

        assertNull(CardPositionManager.lastClickedCardBounds)
        assertNull(CardPositionManager.lastClickedCardCenter)
        assertNull(CardPositionManager.lastClickedNativeCardLayer)
        assertNull(CardPositionManager.lastClickedNativeCardBitmap)
        assertNull(CardPositionManager.lastClickedNativeCoverOverlayLayer)
    }

    @Test
    fun recordVideoCardPosition_preservesHomeCategorySourceKey() {
        CardPositionManager.recordVideoCardPosition(
            bvid = "BV1xx411c7mD",
            sourceRoute = "home?category=POPULAR",
            bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 100f),
            screenWidth = 200f,
            screenHeight = 200f
        )

        assertEquals(
            "home?category=POPULAR:BV1xx411c7mD",
            CardPositionManager.lastClickedVideoSourceKey
        )
    }
}
