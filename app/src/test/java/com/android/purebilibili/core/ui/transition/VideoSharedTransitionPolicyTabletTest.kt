package com.android.purebilibili.core.ui.transition

import com.android.purebilibili.core.ui.adaptive.AdaptiveFoldPosture
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSharedTransitionPolicyTabletTest {

    @Test
    fun durationScalesUpOnExpandedWidth() {
        val speedSettings = VideoSharedTransitionSpeedSettings(speed = VideoSharedTransitionSpeed.STANDARD)
        val compact = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.Compact)
        )
        val expanded = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.Expanded)
        )
        val extraLarge = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.ExtraLarge)
        )

        assertEquals(360, compact)
        assertTrue(expanded > compact, "Expanded should be slower than Compact")
        assertTrue(extraLarge > expanded, "ExtraLarge should be slower than Expanded")
    }

    @Test
    fun durationScalesDownOnFoldPosture() {
        val speedSettings = VideoSharedTransitionSpeedSettings(speed = VideoSharedTransitionSpeed.STANDARD)
        val flat = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                foldPosture = AdaptiveFoldPosture.Flat
            )
        )
        val book = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                foldPosture = AdaptiveFoldPosture.Book
            )
        )
        val tabletop = resolveVideoSharedTransitionDurationMillis(
            speedSettings,
            VideoTransitionAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                foldPosture = AdaptiveFoldPosture.Tabletop
            )
        )

        assertTrue(book < flat, "Book posture should reduce duration")
        assertTrue(tabletop < flat, "Tabletop posture should reduce duration")
        assertEquals(book, tabletop, "Book and Tabletop should share the same reduction factor")
    }

    @Test
    fun coverSharedBoundsDisabledOnFoldPosture() {
        val visualSpecFlat = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            adaptiveInfo = VideoTransitionAdaptiveInfo(foldPosture = AdaptiveFoldPosture.Flat)
        )
        val visualSpecBook = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            adaptiveInfo = VideoTransitionAdaptiveInfo(foldPosture = AdaptiveFoldPosture.Book)
        )
        val visualSpecTabletop = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            adaptiveInfo = VideoTransitionAdaptiveInfo(foldPosture = AdaptiveFoldPosture.Tabletop)
        )

        assertTrue(visualSpecFlat.useCoverSharedBounds)
        assertFalse(visualSpecBook.useCoverSharedBounds)
        assertFalse(visualSpecTabletop.useCoverSharedBounds)
    }

    @Test
    fun motionSpecInheritsAdaptiveDuration() {
        val speedSettings = VideoSharedTransitionSpeedSettings(speed = VideoSharedTransitionSpeed.STANDARD)
        val compactMotion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = speedSettings,
            adaptiveInfo = VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.Compact)
        )
        val largeMotion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = speedSettings,
            adaptiveInfo = VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.Large)
        )

        assertTrue(largeMotion.durationMillis > compactMotion.durationMillis)
        assertEquals(
            resolveVideoSharedTransitionDurationMillis(
                speedSettings,
                VideoTransitionAdaptiveInfo(widthSizeClass = WindowWidthSizeClass.Large)
            ),
            largeMotion.durationMillis
        )
    }
}
