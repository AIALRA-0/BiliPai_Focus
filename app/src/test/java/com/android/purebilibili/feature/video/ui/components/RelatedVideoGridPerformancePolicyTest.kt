package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoGridPerformancePolicyTest {

    @Test
    fun relatedCardsUseOneWholeCardSharedBoundsWithoutPerItemFrameDelays() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/RelatedVideoItem.kt"
        ).readText()
        val gridRowSource = source
            .substringAfter("fun RelatedVideoGridRow(")
            .substringBefore("val pendingVideo = actionVideo")

        assertFalse(gridRowSource.contains("transitionEnabled"))
        assertFalse(gridRowSource.contains("isListScrolling"))
        assertFalse(source.contains("forceSharedTransitionForClick"))
        assertFalse(source.contains("withFrameNanos { }"))
        assertTrue(source.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(source.contains("videoTitleSharedElementKey("))
        assertFalse(source.contains("videoCoverSharedBoundsOrEmpty("))
    }

    @Test
    fun relatedCard_defersBoundsCalculationAndKeepsCoverRequestStableWhileScrolling() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/RelatedVideoItem.kt"
        ).readText()

        assertTrue(source.contains("var value: LayoutCoordinates? = null"))
        assertTrue(source.contains("?.boundsInRoot()"))
        assertTrue(source.contains("cardCoordinatesRef.value = coordinates"))
        assertTrue(source.contains("coverCoordinatesRef.value = coordinates"))
        assertFalse(source.contains("cardBoundsRef.value = coordinates.boundsInRoot()"))
        assertTrue(source.contains("val coverRequest = remember(stationaryCoverUrl)"))
        assertTrue(source.contains("sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE"))
        assertTrue(source.contains("sourceChromeSnapshot = VideoCardSourceChromeSnapshot("))
        assertTrue(source.contains(".withMeasuredCoverDecodeSize(sourceCoverBounds)"))
        assertTrue(source.contains(".then(nativeCardSnapshot.coverOverlayModifier)"))
        assertTrue(source.contains("showDurationOnCover = true"))
    }
}
