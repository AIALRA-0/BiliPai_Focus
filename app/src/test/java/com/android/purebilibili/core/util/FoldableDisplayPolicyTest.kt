package com.android.purebilibili.core.util

import android.content.res.Configuration
import android.view.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FoldableDisplayPolicyTest {
    @Test
    fun `landscape-natural cover remains a cover in both current orientations`() {
        val landscape = puraCover(
            currentWidthDp = 616,
            currentHeightDp = 421,
            configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
            displayRotation = Surface.ROTATION_0,
        )
        val portrait = puraCover(
            currentWidthDp = 421,
            currentHeightDp = 616,
            configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
            displayRotation = Surface.ROTATION_90,
        )

        listOf(landscape, portrait).forEach { context ->
            assertEquals(AppFoldableDisplayRole.Cover, context.foldableDisplayRole)
            assertEquals(AppFoldableDetectionBasis.HingeAngleSensor, context.detectionBasis)
            assertEquals(AppDisplayNaturalOrientation.Landscape, context.naturalOrientation)
            assertTrue(context.usesInWindowFullscreen)
        }
    }

    @Test
    fun `Pura inner display is not mistaken for its cover`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 861,
                currentWindowHeightDp = 609,
                maximumWindowWidthDp = 861,
                maximumWindowHeightDp = 609,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 2584,
                displayModeHeightPx = 1828,
                hasCurrentFoldingFeature = true,
                hasHingeAngleSensor = true,
            )
        )

        assertEquals(AppFoldableDisplayRole.Inner, context.foldableDisplayRole)
        assertEquals(AppFoldableDetectionBasis.CurrentFoldingFeature, context.detectionBasis)
        assertFalse(context.usesInWindowFullscreen)
    }

    @Test
    fun `ordinary portrait-natural phone remains standard`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 397,
                currentWindowHeightDp = 844,
                maximumWindowWidthDp = 397,
                maximumWindowHeightDp = 844,
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 904,
                displayModeHeightPx = 2316,
            )
        )

        assertEquals(AppFoldableDisplayRole.Standard, context.foldableDisplayRole)
        assertEquals(AppDisplayNaturalOrientation.Portrait, context.naturalOrientation)
        assertFalse(context.usesInWindowFullscreen)
    }

    @Test
    fun `portrait-natural foldable cover keeps orientation-driven fullscreen`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 397,
                currentWindowHeightDp = 844,
                maximumWindowWidthDp = 660,
                maximumWindowHeightDp = 884,
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 904,
                displayModeHeightPx = 2316,
                hasHingeAngleSensor = true,
            )
        )

        assertEquals(AppFoldableDisplayRole.Cover, context.foldableDisplayRole)
        assertEquals(AppDisplayNaturalOrientation.Portrait, context.naturalOrientation)
        assertFalse(context.usesInWindowFullscreen)
    }

    @Test
    fun `portrait-held Pura cover with wide displayMode resolves as portrait-natural`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 460,
                currentWindowHeightDp = 672,
                maximumWindowWidthDp = 940,
                maximumWindowHeightDp = 665,
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 1848,
                displayModeHeightPx = 1264,
                hasHingeAngleSensor = true,
            )
        )

        assertEquals(AppFoldableDisplayRole.Cover, context.foldableDisplayRole)
        assertEquals(AppDisplayNaturalOrientation.Portrait, context.naturalOrientation)
        assertFalse(context.usesInWindowFullscreen)
    }

    @Test
    fun `multi-window foldable does not infer a physical display from resized bounds`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 420,
                currentWindowHeightDp = 700,
                maximumWindowWidthDp = 861,
                maximumWindowHeightDp = 609,
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_90,
                hasHingeAngleSensor = true,
                isInMultiWindowMode = true,
            )
        )

        assertEquals(AppFoldableDisplayRole.UnknownFoldable, context.foldableDisplayRole)
        assertFalse(context.usesInWindowFullscreen)
    }

    @Test
    fun `metrics fallback is disabled for ordinary split-screen windows`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 420,
                currentWindowHeightDp = 700,
                maximumWindowWidthDp = 1200,
                maximumWindowHeightDp = 800,
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_0,
                isInMultiWindowMode = true,
            )
        )

        assertEquals(AppFoldableDisplayRole.Standard, context.foldableDisplayRole)
    }

    @Test
    fun `foldable capability keeps large-screen defaults available on a compact cover`() {
        assertTrue(
            resolveLargeScreenOrFoldableConfiguration(
                smallestScreenWidthDp = 421,
                hasHingeAngleSensor = true,
            )
        )
        assertFalse(
            resolveLargeScreenOrFoldableConfiguration(
                smallestScreenWidthDp = 421,
                hasHingeAngleSensor = false,
            )
        )
        assertTrue(
            resolveLargeScreenOrFoldableConfiguration(
                smallestScreenWidthDp = 700,
                hasHingeAngleSensor = false,
            )
        )
    }

    private fun puraCover(
        currentWidthDp: Int,
        currentHeightDp: Int,
        configurationOrientation: Int,
        displayRotation: Int,
    ): AppDisplayContext = resolveAppDisplayContext(
        AppDisplayContextInput(
            currentWindowWidthDp = currentWidthDp,
            currentWindowHeightDp = currentHeightDp,
            maximumWindowWidthDp = 861,
            maximumWindowHeightDp = 609,
            configurationOrientation = configurationOrientation,
            displayRotation = displayRotation,
            displayModeWidthPx = 1848,
            displayModeHeightPx = 1264,
            hasHingeAngleSensor = true,
        )
    )
}
