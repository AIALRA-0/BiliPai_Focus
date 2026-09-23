package com.android.purebilibili.feature.search

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveUiStyle
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.resolveAppTopChromePolicy
import com.android.purebilibili.feature.home.components.resolveSharedBottomBarCapsuleShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChromePolicyTest {

    @Test
    fun `md3 preset should use taller search chrome and filled action`() {
        val spec = resolveSearchChromeVisualSpec(
            resolveAppTopChromePolicy(resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MATERIAL3))
        )

        assertEquals(56, spec.inputHeightDp)
        assertEquals(ContainerLevel.Pill, spec.inputShapeLevel)
        assertEquals(ContainerLevel.Pill, spec.actionShapeLevel)
        assertEquals(ContainerLevel.Card, spec.suggestionShapeLevel)
        assertEquals(ContainerLevel.Pill, spec.chipShapeLevel)
        assertEquals(48, spec.clearActionSizeDp)
        assertEquals(48, spec.submitActionSizeDp)
        assertEquals(24, spec.actionIconSizeDp)
        assertEquals(12, spec.horizontalGapDp)
        assertTrue(spec.useFilledSearchAction)
    }

    @Test
    fun `legacy ios preset maps to miuix search chrome`() {
        // 2B 迁移：iOS 输入经迁移表并入 MIUIX，与 miuix 呈现一致。
        val spec = resolveSearchChromeVisualSpec(
            resolveAppTopChromePolicy(resolveUiStyle(UiPreset.IOS, AndroidNativeVariant.MATERIAL3))
        )

        assertEquals(48, spec.inputHeightDp)
        assertEquals(ContainerLevel.Pill, spec.inputShapeLevel)
        assertEquals(ContainerLevel.Pill, spec.actionShapeLevel)
        assertEquals(48, spec.clearActionSizeDp)
        assertEquals(48, spec.submitActionSizeDp)
        assertEquals(14, spec.inputHorizontalPaddingDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(ContainerLevel.Card, spec.suggestionShapeLevel)
    }

    @Test
    fun `miuix variant should use denser rounded search chrome`() {
        val spec = resolveSearchChromeVisualSpec(
            resolveAppTopChromePolicy(resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MIUIX))
        )

        assertEquals(48, spec.inputHeightDp)
        assertEquals(ContainerLevel.Pill, spec.inputShapeLevel)
        assertEquals(ContainerLevel.Pill, spec.actionShapeLevel)
        assertEquals(48, spec.clearActionSizeDp)
        assertEquals(48, spec.submitActionSizeDp)
        assertEquals(14, spec.inputHorizontalPaddingDp)
        assertTrue(spec.useFilledSearchAction)
        assertEquals(ContainerLevel.Card, spec.suggestionShapeLevel)
        assertEquals(ContainerLevel.Pill, spec.chipShapeLevel)
    }

    @Test
    fun `search input shape follows result type capsule`() {
        val md3 = resolveAppTopChromePolicy(resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MATERIAL3))
        val miuix = resolveAppTopChromePolicy(resolveUiStyle(UiPreset.MD3, AndroidNativeVariant.MIUIX))

        assertEquals(resolveSharedBottomBarCapsuleShape(), resolveSearchInputShape(md3))
        assertEquals(resolveSharedBottomBarCapsuleShape(), resolveSearchInputShape(miuix))
    }

    @Test
    fun `global wallpaper makes search top bar protected but translucent`() {
        assertEquals(
            Color.White.copy(alpha = 0.96f),
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
    fun `search header blur requires the preference and a usable source`() {
        assertFalse(
            shouldUseSearchTopBarHeaderBlur(
                headerBlurRequested = false,
                hazeSourceEnabled = true,
                globalWallpaperVisible = false
            )
        )
        assertFalse(
            shouldUseSearchTopBarHeaderBlur(
                headerBlurRequested = true,
                hazeSourceEnabled = false,
                globalWallpaperVisible = false
            )
        )
        assertTrue(
            shouldUseSearchTopBarHeaderBlur(
                headerBlurRequested = true,
                hazeSourceEnabled = true,
                globalWallpaperVisible = false
            )
        )
    }

    @Test
    fun `disabled top effects select an opaque search chrome independently of liquid glass`() {
        assertTrue(
            shouldUseSearchSolidTopChrome(
                headerBlurRequested = false,
                progressiveBlurRequested = false,
            )
        )
        assertFalse(
            shouldUseSearchSolidTopChrome(
                headerBlurRequested = true,
                progressiveBlurRequested = false,
            )
        )
        assertFalse(
            shouldUseSearchSolidTopChrome(
                headerBlurRequested = false,
                progressiveBlurRequested = true,
            )
        )
    }

    @Test
    fun `global wallpaper disables search header blur`() {
        assertFalse(
            shouldUseSearchTopBarHeaderBlur(
                headerBlurRequested = true,
                hazeSourceEnabled = true,
                globalWallpaperVisible = true
            )
        )
        assertTrue(
            shouldUseSearchTopBarHeaderBlur(
                headerBlurRequested = true,
                hazeSourceEnabled = true,
                globalWallpaperVisible = false
            )
        )
    }
}
