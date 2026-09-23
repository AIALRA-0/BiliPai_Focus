package com.android.purebilibili.navigation3

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavSourceMetadataPolicyTest {

    @Test
    fun rootBoundsAreTranslatedIntoNavigationHostCoordinates() {
        val metadata = BiliPaiNavSourceMetadata(
            sourceBounds = Rect(180f, 240f, 480f, 440f),
            sourceCoverBounds = Rect(180f, 240f, 480f, 380f),
        ).relativeToHost(Offset(120f, 40f))

        assertEquals(Rect(60f, 200f, 360f, 400f), metadata.sourceBounds)
        assertEquals(Rect(60f, 200f, 360f, 340f), metadata.sourceCoverBounds)
    }

    @Test
    fun clickedVisibleCardIsSharedTransitionReady() {
        val metadata = BiliPaiNavSourceMetadata(
            sourceKey = "home:BV1",
            sourceRoute = "home",
            clickedBoundsRecorded = true,
            cardFullyVisible = true
        )

        assertTrue(metadata.sharedTransitionEntryReady)
        assertTrue(metadata.sharedTransitionReady)
        assertEquals("home", metadata.sourceRoute)
        assertEquals("home:BV1", metadata.sourceKey)
    }

    @Test
    fun missingBoundsOrInvisibleCardUsesFallbackMotion() {
        assertFalse(
            BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = false,
                cardFullyVisible = true
            ).sharedTransitionEntryReady
        )
        assertFalse(
            BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = false,
                cardFullyVisible = true
            ).sharedTransitionReady
        )
        val partiallyVisibleCard = BiliPaiNavSourceMetadata(
            sourceKey = "home:BV1",
            sourceRoute = "home",
            clickedBoundsRecorded = true,
            cardFullyVisible = false
        )
        assertTrue(partiallyVisibleCard.sharedTransitionEntryReady)
        assertFalse(partiallyVisibleCard.sharedTransitionReady)
    }

    @Test
    fun resolverKeepsSourceRouteQueryForTopTabScopedVideoSource() {
        val metadata = resolveBiliPaiNavSourceMetadata(
            sourceKey = "home?category=POPULAR:BV1",
            sourceRoute = "home?category=POPULAR",
            clickedBoundsRecorded = true,
            cardFullyVisible = true
        )

        assertEquals("home?category=POPULAR", metadata.sourceRoute)
        assertEquals("home?category=POPULAR:BV1", metadata.sourceKey)
        assertTrue(metadata.sharedTransitionEntryReady)
        assertTrue(metadata.sharedTransitionReady)
    }

    @Test
    fun cardSourceDirection_splitsDualColumnAtMidScreenEvenWhenPartiallyVisible() {
        assertEquals(
            BiliPaiNavCardSourceDirection.SOURCE_LEFT,
            resolveBiliPaiNavCardSourceDirection(
                clickedBoundsRecorded = true,
                cardFullyVisible = false,
                isSingleColumnCard = false,
                normalizedCenterX = 0.25f
            )
        )
        assertEquals(
            BiliPaiNavCardSourceDirection.SOURCE_RIGHT,
            resolveBiliPaiNavCardSourceDirection(
                clickedBoundsRecorded = true,
                cardFullyVisible = false,
                isSingleColumnCard = false,
                normalizedCenterX = 0.75f
            )
        )
        assertEquals(
            BiliPaiNavCardSourceDirection.NONE,
            resolveBiliPaiNavCardSourceDirection(
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                isSingleColumnCard = true,
                normalizedCenterX = 0.2f
            )
        )
    }

    @Test
    fun effectiveDirection_prefersLiveThenSession() {
        assertEquals(
            BiliPaiNavCardSourceDirection.SOURCE_LEFT,
            resolveEffectiveCardSourceDirection(
                liveDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT,
                sessionDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT
            )
        )
        assertEquals(
            BiliPaiNavCardSourceDirection.SOURCE_RIGHT,
            resolveEffectiveCardSourceDirection(
                liveDirection = BiliPaiNavCardSourceDirection.NONE,
                sessionDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT
            )
        )
    }

    @Test
    fun videoSourceKeysCompatible_matchesBvidSuffixAcrossCategoryKeys() {
        assertTrue(
            areVideoSourceKeysCompatible(
                cardKey = "home?category=POPULAR:BV1xx",
                sessionKey = "home:BV1xx"
            )
        )
        assertTrue(
            areVideoSourceKeysCompatible(
                cardKey = "home:BV1xx",
                sessionKey = "home:BV1xx"
            )
        )
        assertFalse(
            areVideoSourceKeysCompatible(
                cardKey = "home:BV1",
                sessionKey = "home:BV2"
            )
        )
    }
}
