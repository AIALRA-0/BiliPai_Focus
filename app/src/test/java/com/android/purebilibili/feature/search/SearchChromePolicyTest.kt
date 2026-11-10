package com.android.purebilibili.feature.search

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.resolveAppTopChromePolicy
import com.android.purebilibili.feature.home.components.resolveHomeTopSearchContainerShape
import com.android.purebilibili.feature.home.components.resolveHomeTopSearchPillHeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChromePolicyTest {

    @Test
    fun `search fields keep rounded corners instead of semicircular ends at any width`() {
        for (style in AppUiStyle.entries) {
            val chrome = resolveAppTopChromePolicy(style)
            val fields = listOf(
                resolveSearchInputShape(chrome) to resolveSearchChromeVisualSpec(chrome).inputHeightDp.toFloat(),
                resolveHomeTopSearchContainerShape(chrome) to resolveHomeTopSearchPillHeight(chrome).value,
            )
            for ((shape, heightDp) in fields) {
                for (density in listOf(Density(1f), Density(2.75f))) {
                    for (direction in LayoutDirection.entries) {
                        val heightPx = heightDp * density.density
                        val narrow = shape.createOutline(Size(200f * density.density, heightPx), direction, density) as Outline.Rounded
                        val wide = shape.createOutline(Size(720f * density.density, heightPx), direction, density) as Outline.Rounded
                        val corners = listOf(
                            narrow.roundRect.topLeftCornerRadius,
                            narrow.roundRect.topRightCornerRadius,
                            narrow.roundRect.bottomLeftCornerRadius,
                            narrow.roundRect.bottomRightCornerRadius,
                        )
                        for (corner in corners) {
                            assertTrue("Search ends must not become a pill: $style", corner.x > 0f && corner.x < heightPx / 2f)
                            assertEquals(corner.x, corner.y, 0.001f)
                        }
                        assertEquals(narrow.roundRect.topLeftCornerRadius, wide.roundRect.topLeftCornerRadius)
                    }
                }
            }
        }
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
