package com.android.purebilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashExitAnimationPolicyTest {

    @Test
    fun clipsSplashFlyoutToLauncherStyleRoundedSquare() {
        assertEquals(24f, splashFlyoutCornerRadiusPx(sizePx = 100), 0.001f)
        assertEquals(48f, splashFlyoutCornerRadiusPx(sizePx = 200), 0.001f)
    }

    @Test
    fun enablesRealtimeBlurForSmallDedicatedIconLayersOnAndroid12Through15() {
        assertTrue(shouldUseRealtimeSplashBlur(31))
        assertTrue(shouldUseRealtimeSplashBlur(33))
        assertTrue(shouldUseRealtimeSplashBlur(34))
    }

    @Test
    fun disablesRealtimeBlurOnSamsungAndroid13() {
        assertFalse(shouldUseRealtimeSplashBlur(sdkInt = 33, manufacturer = "samsung"))
        assertFalse(shouldUseRealtimeSplashBlur(sdkInt = 33, manufacturer = "SAMSUNG"))
        assertTrue(shouldUseRealtimeSplashBlur(sdkInt = 34, manufacturer = "samsung"))
        assertTrue(shouldUseRealtimeSplashBlur(sdkInt = 33, manufacturer = "Xiaomi"))
        assertTrue(shouldUseRealtimeSplashBlur(sdkInt = 33, manufacturer = "Google"))
    }

    @Test
    fun disablesRealtimeBlurOnAndroid16RenderThread() {
        assertFalse(shouldUseRealtimeSplashBlur(36))
    }

    @Test
    fun disablesRealtimeBlurBelowAndroid12() {
        assertFalse(shouldUseRealtimeSplashBlur(30))
    }

    @Test
    fun flyoutTargetSize_acceptsNormalSquareSystemIcon() {
        assertEquals(
            224,
            resolveSplashFlyoutTargetSizePx(
                systemIconWidthPx = 224,
                systemIconHeightPx = 224,
                density = 2f,
            )
        )
    }

    @Test
    fun flyoutTargetSize_rejectsOemFullscreenRectangleAndUsesSquareFallback() {
        assertEquals(
            224,
            resolveSplashFlyoutTargetSizePx(
                systemIconWidthPx = 700,
                systemIconHeightPx = 1536,
                density = 2f,
            )
        )
    }

    @Test
    fun allowsCustomSplashOverlayWhenFlyoutEnabledAndDataPresent() {
        assertTrue(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg"
            )
        )
    }

    @Test
    fun allowsCustomSplashOverlayWhenFlyoutDisabledAndDataPresent() {
        assertTrue(
            shouldShowCustomSplashOverlay(
                customSplashEnabled = true,
                splashUri = "content://splash.jpg"
            )
        )
    }

    @Test
    fun appliesRealtimeBlurOnlyAfterAnimationProgressStarts() {
        assertFalse(shouldApplySplashRealtimeBlur(useRealtimeBlur = true, progress = 0f))
        assertFalse(shouldApplySplashRealtimeBlur(useRealtimeBlur = true, progress = 0.1f))
        assertTrue(shouldApplySplashRealtimeBlur(useRealtimeBlur = true, progress = 0.12f))
        assertFalse(shouldApplySplashRealtimeBlur(useRealtimeBlur = false, progress = 0.5f))
    }
}
