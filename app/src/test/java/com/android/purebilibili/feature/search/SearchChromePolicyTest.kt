package com.android.purebilibili.feature.search

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChromePolicyTest {

    @Test
    fun `md3 preset should use taller search chrome and filled action`() {
        val spec = resolveSearchChromeVisualSpec(UiPreset.MD3)

        assertEquals(44, spec.inputHeightDp)
        assertEquals(22, spec.inputCornerRadiusDp)
        assertEquals(40, spec.clearActionSizeDp)
        assertEquals(40, spec.submitActionSizeDp)
        assertEquals(20, spec.actionIconSizeDp)
        assertEquals(8, spec.horizontalGapDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(20, spec.suggestionContainerCornerRadiusDp)
    }

    @Test
    fun `ios preset should preserve compact capsule search chrome`() {
        val spec = resolveSearchChromeVisualSpec(UiPreset.IOS)

        assertEquals(44, spec.inputHeightDp)
        assertEquals(22, spec.inputCornerRadiusDp)
        assertEquals(40, spec.clearActionSizeDp)
        assertEquals(40, spec.submitActionSizeDp)
        assertEquals(20, spec.actionIconSizeDp)
        assertEquals(12, spec.inputHorizontalPaddingDp)
        assertFalse(spec.useFilledSearchAction)
        assertEquals(12, spec.suggestionContainerCornerRadiusDp)
    }

    @Test
    fun `miuix variant should use denser rounded search chrome`() {
        val spec = resolveSearchChromeVisualSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(44, spec.inputHeightDp)
        assertEquals(22, spec.inputCornerRadiusDp)
        assertEquals(40, spec.clearActionSizeDp)
        assertEquals(40, spec.submitActionSizeDp)
        assertEquals(14, spec.inputHorizontalPaddingDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(18, spec.suggestionContainerCornerRadiusDp)
    }

    @Test
    fun `global wallpaper makes search top bar transparent`() {
        assertEquals(
            Color.Transparent,
            resolveSearchTopBarHeaderColor(
                surfaceColor = Color.White,
                backgroundAlpha = 0.96f,
                globalWallpaperVisible = true,
                useHeaderBlur = false
            )
        )
    }

    @Test
    fun `search top bar keeps fallback surface without wallpaper or blur`() {
        assertEquals(
            Color.White.copy(alpha = 0.96f),
            resolveSearchTopBarHeaderColor(
                surfaceColor = Color.White,
                backgroundAlpha = 0.96f,
                globalWallpaperVisible = false,
                useHeaderBlur = false
            )
        )
    }

    @Test
    fun `global wallpaper disables search header blur`() {
        assertFalse(
            shouldUseSearchTopBarHeaderBlur(
                hazeSourceEnabled = true,
                globalWallpaperVisible = true
            )
        )
        assertTrue(
            shouldUseSearchTopBarHeaderBlur(
                hazeSourceEnabled = true,
                globalWallpaperVisible = false
            )
        )
    }
}
