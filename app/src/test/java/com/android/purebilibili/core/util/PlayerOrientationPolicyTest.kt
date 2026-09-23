package com.android.purebilibili.core.util

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerOrientationPolicyTest {
    @Test
    fun `Pura style landscape natural cover uses in window fullscreen in both rotations`() {
        val landscape = resolvePlayerWindowOrientationPolicy(
            smallestScreenWidthDp = 421,
            currentWindowWidthDp = 616,
            currentWindowHeightDp = 421,
            maximumWidthDp = 861,
            maximumHeightDp = 609,
            configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
            displayRotation = Surface.ROTATION_0,
            displayModeWidthPx = 1848,
            displayModeHeightPx = 1264,
        )
        val portrait = resolvePlayerWindowOrientationPolicy(
            smallestScreenWidthDp = 421,
            currentWindowWidthDp = 421,
            currentWindowHeightDp = 616,
            maximumWidthDp = 861,
            maximumHeightDp = 609,
            configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
            displayRotation = Surface.ROTATION_90,
            displayModeWidthPx = 1848,
            displayModeHeightPx = 1264,
        )

        assertTrue(landscape.isFoldableCoverWindow)
        assertTrue(landscape.isLandscapeNaturalDisplay)
        assertTrue(landscape.usesInWindowFullscreen)
        assertTrue(portrait.isFoldableCoverWindow)
        assertTrue(portrait.isLandscapeNaturalDisplay)
        assertTrue(portrait.usesInWindowFullscreen)
    }

    @Test
    fun `portrait natural cover keeps phone orientation driven fullscreen`() {
        val policy = resolvePlayerWindowOrientationPolicy(
            smallestScreenWidthDp = 397,
            currentWindowWidthDp = 844,
            currentWindowHeightDp = 397,
            maximumWidthDp = 884,
            maximumHeightDp = 660,
            configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
            displayRotation = Surface.ROTATION_90,
            displayModeWidthPx = 904,
            displayModeHeightPx = 2316,
        )

        assertTrue(policy.isFoldableCoverWindow)
        assertFalse(policy.isLandscapeNaturalDisplay)
        assertFalse(policy.usesInWindowFullscreen)
    }

    @Test
    fun `Pura style inner display is not treated as cover`() {
        val policy = resolvePlayerWindowOrientationPolicy(
            smallestScreenWidthDp = 609,
            currentWindowWidthDp = 861,
            currentWindowHeightDp = 609,
            maximumWidthDp = 861,
            maximumHeightDp = 609,
            configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
            displayRotation = Surface.ROTATION_0,
            displayModeWidthPx = 2584,
            displayModeHeightPx = 1828,
        )

        assertFalse(policy.isFoldableCoverWindow)
        assertTrue(policy.isLandscapeNaturalDisplay)
        assertFalse(policy.usesInWindowFullscreen)
    }

    @Test
    fun `landscape natural cover suppresses axis requests but preserves lock and cleanup`() {
        assertFalse(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 421,
                currentWindowWidthDp = 616,
                currentWindowHeightDp = 421,
                maximumWidthDp = 861,
                maximumHeightDp = 609,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 1848,
                displayModeHeightPx = 1264,
            )
        )
        listOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ).forEach { requestedOrientation ->
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                resolveEffectivePlayerRequestedOrientation(
                    requestedOrientation = requestedOrientation,
                    usesInWindowFullscreen = true,
                )
            )
        }
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LOCKED,
            resolveEffectivePlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED,
                usesInWindowFullscreen = true,
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveEffectivePlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                usesInWindowFullscreen = true,
            )
        )
    }

    @Test
    fun `display mode keeps landscape natural cover detectable while activity is portrait letterboxed`() {
        assertTrue(
            isLandscapeNaturalDisplay(
                configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                displayRotation = Surface.ROTATION_0,
                displayModeWidthPx = 1848,
                displayModeHeightPx = 1264,
            )
        )
    }

    @Test
    fun `hinge sensor still identifies a cover when vendor maximum metrics match the outer display`() {
        val policy = resolvePlayerWindowOrientationPolicy(
            resolveAppDisplayContext(
                AppDisplayContextInput(
                    currentWindowWidthDp = 421,
                    currentWindowHeightDp = 616,
                    maximumWindowWidthDp = 616,
                    maximumWindowHeightDp = 421,
                    configurationOrientation = Configuration.ORIENTATION_PORTRAIT,
                    displayRotation = Surface.ROTATION_0,
                    displayModeWidthPx = 1848,
                    displayModeHeightPx = 1264,
                    hasHingeAngleSensor = true,
                )
            )
        )

        assertTrue(policy.isFoldableCoverWindow)
        assertTrue(policy.usesInWindowFullscreen)
    }

    @Test
    fun `large foldable cover window is distinguished from its inner display`() {
        assertTrue(
            isFoldableCoverWindow(
                smallestScreenWidthDp = 665,
                currentWindowWidthDp = 672,
                currentWindowHeightDp = 459,
            )
        )
        assertFalse(
            isFoldableCoverWindow(
                smallestScreenWidthDp = 665,
                currentWindowWidthDp = 940,
                currentWindowHeightDp = 665,
            )
        )
    }

    @Test
    fun `physical orientation remains available below 600dp`() {
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 599,
                platformIgnoresLargeScreenOrientationRequests = true,
            )
        )
    }

    @Test
    fun `pre Android 16 tablets retain direct fullscreen rotation`() {
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 600,
                platformIgnoresLargeScreenOrientationRequests = false,
            )
        )
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 720,
                platformIgnoresLargeScreenOrientationRequests = false,
            )
        )
    }

    @Test
    fun `Android 16 plus large screens preserve player orientation intent`() {
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 600,
                platformIgnoresLargeScreenOrientationRequests = true,
            )
        )
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 720,
                platformIgnoresLargeScreenOrientationRequests = true,
            )
        )
    }

    @Test
    fun `Android 16 plus foldable cover window retains physical orientation requests`() {
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 720,
                currentWindowWidthDp = 672,
                currentWindowHeightDp = 459,
                platformIgnoresLargeScreenOrientationRequests = true,
            )
        )
    }

    @Test
    fun `foldable cover window with small current swDp but large maximum window is recognized`() {
        assertTrue(
            isFoldableCoverWindow(
                smallestScreenWidthDp = 421,
                currentWindowWidthDp = 616,
                currentWindowHeightDp = 421,
                maximumWidthDp = 861,
                maximumHeightDp = 609,
            )
        )
        assertFalse(
            isFoldableCoverWindow(
                smallestScreenWidthDp = 421,
                currentWindowWidthDp = 861,
                currentWindowHeightDp = 609,
                maximumWidthDp = 861,
                maximumHeightDp = 609,
            )
        )
        assertTrue(
            shouldRequestPhysicalPlayerOrientation(
                smallestScreenWidthDp = 421,
                currentWindowWidthDp = 616,
                currentWindowHeightDp = 421,
                maximumWidthDp = 861,
                maximumHeightDp = 609,
                platformIgnoresLargeScreenOrientationRequests = true,
            )
        )
    }

    @Test
    fun `display context maps to player policy without Activity overload recursion`() {
        val context = resolveAppDisplayContext(
            AppDisplayContextInput(
                currentWindowWidthDp = 1280,
                currentWindowHeightDp = 800,
                maximumWindowWidthDp = 1280,
                maximumWindowHeightDp = 800,
                configurationOrientation = Configuration.ORIENTATION_LANDSCAPE,
                displayRotation = Surface.ROTATION_90,
            )
        )
        val policy = context.toPlayerWindowOrientationPolicy()
        assertEquals(1280, policy.currentWindowWidthDp)
        assertEquals(800, policy.currentWindowHeightDp)
        assertFalse(policy.usesInWindowFullscreen)

        val source = java.io.File(
            "app/src/main/java/com/android/purebilibili/core/util/PlayerOrientationPolicy.kt"
        ).takeIf { it.exists() } ?: java.io.File(
            "src/main/java/com/android/purebilibili/core/util/PlayerOrientationPolicy.kt"
        )
        val activityOverload = source.readText()
            .substringAfter("internal fun Activity.resolvePlayerWindowOrientationPolicy(")
            .substringBefore("private fun isPlayerAxisOrientationRequest(")
        assertTrue(activityOverload.contains("toPlayerWindowOrientationPolicy()"))
        assertFalse(
            activityOverload.contains("return resolvePlayerWindowOrientationPolicy("),
            "Activity overload must not recurse into itself",
        )
    }
}
