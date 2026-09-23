package com.android.purebilibili.core.util

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerPresentationPolicyTest {

    @Test
    fun `Pura X Max cover stays in-window across landscape, portrait grip, and letterboxed lock`() {
        listOf(
            puraCover(widthDp = 616, heightDp = 421, orientation = Configuration.ORIENTATION_LANDSCAPE, rotation = Surface.ROTATION_0),
            puraCover(widthDp = 421, heightDp = 616, orientation = Configuration.ORIENTATION_PORTRAIT, rotation = Surface.ROTATION_90),
            puraCover(widthDp = 421, heightDp = 616, orientation = Configuration.ORIENTATION_PORTRAIT, rotation = Surface.ROTATION_0, orientationConstrained = true),
        ).forEach { displayContext ->
            val presentation = resolvePlayerPresentationPolicy(
                displayContext = displayContext,
                isLandscape = displayContext.currentWindowWidthDp > displayContext.currentWindowHeightDp,
                userFullscreenIntent = false,
                prefersManualFullscreen = false,
                isInMultiWindowMode = false,
            )
            assertEquals(AppFoldableDisplayRole.Cover, displayContext.foldableDisplayRole)
            assertTrue(presentation.usesInWindowFullscreen)
            assertFalse(presentation.isOrientationDriven)
            assertFalse(presentation.isFullscreen)
            assertFalse(presentation.shouldHandleBackAsExitFullscreen)
            listOf(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            ).forEach { requested ->
                assertEquals(
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                    resolvePlayerAxisOrientationRequest(
                        requestedOrientation = requested,
                        usesInWindowFullscreen = presentation.usesInWindowFullscreen,
                    ),
                )
            }
        }
    }

    @Test
    fun `user fullscreen on landscape-natural cover is in-window and back exits that layer`() {
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = puraCover(
                widthDp = 616,
                heightDp = 421,
                orientation = Configuration.ORIENTATION_LANDSCAPE,
                rotation = Surface.ROTATION_0,
            ),
            isLandscape = true,
            userFullscreenIntent = true,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertTrue(presentation.usesInWindowFullscreen)
        assertTrue(presentation.userFullscreenIntent)
        assertTrue(presentation.isFullscreen)
        assertTrue(presentation.shouldHandleBackAsExitFullscreen)
        assertFalse(presentation.orientationGeneratedFullscreen)
    }

    @Test
    fun `Pura inner display is not treated as a cover and keeps user-driven fullscreen`() {
        val displayContext = resolveAppDisplayContext(
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
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = true,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertEquals(AppFoldableDisplayRole.Inner, displayContext.foldableDisplayRole)
        assertFalse(presentation.usesInWindowFullscreen)
        assertFalse(presentation.isOrientationDriven)
        assertFalse(presentation.isFullscreen)
    }

    @Test
    fun `ordinary landscape phone still auto-generates fullscreen`() {
        val displayContext = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 844,
                currentWindowHeightDp = 397,
                maximumWindowWidthDp = 397,
                maximumWindowHeightDp = 844,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_90,
                displayModeWidthPx = 904,
                displayModeHeightPx = 2316,
            )
        )
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = true,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertEquals(AppFoldableDisplayRole.Standard, displayContext.foldableDisplayRole)
        assertTrue(presentation.isOrientationDriven)
        assertTrue(presentation.orientationGeneratedFullscreen)
        assertTrue(presentation.isFullscreen)
        assertFalse(presentation.usesInWindowFullscreen)
    }

    @Test
    fun `ordinary tablet does not auto-enter fullscreen from landscape`() {
        val displayContext = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 1280,
                currentWindowHeightDp = 800,
                maximumWindowWidthDp = 1280,
                maximumWindowHeightDp = 800,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_90,
            )
        )
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = true,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertEquals(AppFoldableDisplayRole.Standard, displayContext.foldableDisplayRole)
        assertFalse(presentation.isOrientationDriven)
        assertFalse(presentation.isFullscreen)
    }

    @Test
    fun `portrait-natural foldable cover keeps phone orientation-driven fullscreen`() {
        val displayContext = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 844,
                currentWindowHeightDp = 397,
                maximumWindowWidthDp = 660,
                maximumWindowHeightDp = 884,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_90,
                displayModeWidthPx = 904,
                displayModeHeightPx = 2316,
                hasHingeAngleSensor = true,
            )
        )
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = true,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertEquals(AppFoldableDisplayRole.Cover, displayContext.foldableDisplayRole)
        assertFalse(presentation.usesInWindowFullscreen)
        assertTrue(presentation.isOrientationDriven)
        assertTrue(presentation.orientationGeneratedFullscreen)
    }

    @Test
    fun `split-screen foldable is unknown and does not consume back as fullscreen`() {
        val displayContext = resolveAppDisplayContext(
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
        val presentation = resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = false,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = true,
        )
        assertEquals(AppFoldableDisplayRole.UnknownFoldable, displayContext.foldableDisplayRole)
        assertFalse(presentation.usesInWindowFullscreen)
        assertFalse(presentation.isFullscreen)
        assertFalse(presentation.shouldHandleBackAsExitFullscreen)
        assertFalse(resolvePlayerBackConsumption(isFullscreen = false))
    }

    @Test
    fun `fold and unfold keep user fullscreen intent and drop orientation-generated fullscreen`() {
        val cover = puraCover(
            widthDp = 616,
            heightDp = 421,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            rotation = Surface.ROTATION_0,
        )
        val inner = resolveAppDisplayContext(
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
        val autoOnPhone = resolvePlayerPresentationPolicy(
            displayContext = cover,
            isLandscape = true,
            userFullscreenIntent = false,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertFalse(autoOnPhone.userFullscreenIntent)
        assertEquals(
            false,
            resolveUserFullscreenIntentAfterDisplayRoleChange(
                previousRole = cover.foldableDisplayRole,
                nextRole = inner.foldableDisplayRole,
                previousUserFullscreenIntent = autoOnPhone.userFullscreenIntent,
            ),
        )

        val userOnCover = resolvePlayerPresentationPolicy(
            displayContext = cover,
            isLandscape = true,
            userFullscreenIntent = true,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        val retainedIntent = resolveUserFullscreenIntentAfterDisplayRoleChange(
            previousRole = cover.foldableDisplayRole,
            nextRole = inner.foldableDisplayRole,
            previousUserFullscreenIntent = userOnCover.userFullscreenIntent,
        )
        val afterUnfold = resolvePlayerPresentationPolicy(
            displayContext = inner,
            isLandscape = true,
            userFullscreenIntent = retainedIntent,
            prefersManualFullscreen = false,
            isInMultiWindowMode = false,
        )
        assertTrue(afterUnfold.userFullscreenIntent)
        assertTrue(afterUnfold.isFullscreen)
        assertFalse(afterUnfold.orientationGeneratedFullscreen)
        assertTrue(
            shouldReleaseOrientationLockOnDisplayRoleChange(
                previousRole = cover.foldableDisplayRole,
                nextRole = inner.foldableDisplayRole,
            )
        )
    }

    @Test
    fun `one back press only exits a real fullscreen layer`() {
        assertTrue(resolvePlayerBackConsumption(isFullscreen = true))
        assertFalse(resolvePlayerBackConsumption(isFullscreen = false))
    }

    private fun puraCover(
        widthDp: Int,
        heightDp: Int,
        orientation: Int,
        rotation: Int,
        orientationConstrained: Boolean = false,
    ): AppDisplayContext = resolveAppDisplayContext(
        AppDisplayContextInput(
            currentWindowWidthDp = widthDp,
            currentWindowHeightDp = heightDp,
            maximumWindowWidthDp = 861,
            maximumWindowHeightDp = 609,
            configurationOrientation = orientation,
            displayRotation = rotation,
            displayModeWidthPx = 1848,
            displayModeHeightPx = 1264,
            hasHingeAngleSensor = true,
            hasOrientationConstrainedWindow = orientationConstrained,
        )
    )
}
