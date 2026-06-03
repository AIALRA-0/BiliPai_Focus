package com.android.purebilibili.core.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSSystemGray6
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSurfaceTokensTest {

    private val scheme = lightColorScheme(
        surface = Color.White,
        surfaceContainer = Color(0xFFEEEEEE),
        background = iOSSystemGray6,
        outlineVariant = Color(0xFFC7C7CC)
    )

    @Test
    fun cardContainer_ios_returnsSurface() {
        val color = AppSurfaceTokens.resolveCardContainer(
            colorScheme = scheme,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(Color.White, color)
    }

    @Test
    fun cardContainer_md3_returnsSurfaceContainer() {
        val color = AppSurfaceTokens.resolveCardContainer(
            colorScheme = scheme,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(Color(0xFFEEEEEE), color)
    }

    @Test
    fun cardContainer_miuix_returnsSurfaceContainer() {
        // Miuix variant uses surfaceContainer; bridge maps it to Miuix secondaryContainerVariant.
        val color = AppSurfaceTokens.resolveCardContainer(
            colorScheme = scheme,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        assertEquals(Color(0xFFEEEEEE), color)
    }

    @Test
    fun groupedListContainer_ios_fallsBackToIosSystemGray6() {
        val color = AppSurfaceTokens.resolveGroupedListContainer(
            colorScheme = scheme,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(iOSSystemGray6, color)
    }

    @Test
    fun groupedListContainer_md3_usesBackground() {
        val color = AppSurfaceTokens.resolveGroupedListContainer(
            colorScheme = scheme,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(iOSSystemGray6, color) // because scheme.background = iOSSystemGray6
    }

    @Test
    fun chromeBackground_returnsBackground() {
        val color = AppSurfaceTokens.resolveChromeBackground(
            colorScheme = scheme,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(iOSSystemGray6, color)
    }

    @Test
    fun divider_returnsOutlineVariant() {
        val color = AppSurfaceTokens.resolveDivider(
            colorScheme = scheme,
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        assertEquals(Color(0xFFC7C7CC), color)
    }
}
