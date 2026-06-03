package com.android.purebilibili.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset

/**
 * Preset-aware surface tokens. Replace direct reads of `MaterialTheme.colorScheme.surface`
 * / `.background` in feature screens with these accessors so AMOLED, dynamic-color, and
 * Miuix bridging stay consistent across presets.
 *
 * - iOS:   card = white surface, grouped list = iOSSystemGray6 background.
 * - MD3:   card = surfaceContainer, grouped list = background.
 * - Miuix: card = surfaceContainer (bridge maps it to Miuix's secondaryContainerVariant).
 */
object AppSurfaceTokens {

    fun resolveCardContainer(
        colorScheme: ColorScheme,
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): Color = when (uiPreset) {
        UiPreset.IOS -> colorScheme.surface
        UiPreset.MD3 -> colorScheme.surfaceContainer
    }

    fun resolveGroupedListContainer(
        colorScheme: ColorScheme,
        @Suppress("UNUSED_PARAMETER") uiPreset: UiPreset,
        @Suppress("UNUSED_PARAMETER") androidNativeVariant: AndroidNativeVariant
    ): Color = colorScheme.background

    fun resolveChromeBackground(
        colorScheme: ColorScheme,
        @Suppress("UNUSED_PARAMETER") uiPreset: UiPreset,
        @Suppress("UNUSED_PARAMETER") androidNativeVariant: AndroidNativeVariant
    ): Color = colorScheme.background

    fun resolveDivider(
        colorScheme: ColorScheme,
        @Suppress("UNUSED_PARAMETER") uiPreset: UiPreset,
        @Suppress("UNUSED_PARAMETER") androidNativeVariant: AndroidNativeVariant
    ): Color = colorScheme.outlineVariant

    @Composable
    @ReadOnlyComposable
    fun cardContainer(): Color = resolveCardContainer(
        colorScheme = MaterialTheme.colorScheme,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    @ReadOnlyComposable
    fun groupedListContainer(): Color = resolveGroupedListContainer(
        colorScheme = MaterialTheme.colorScheme,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    @ReadOnlyComposable
    fun chromeBackground(): Color = resolveChromeBackground(
        colorScheme = MaterialTheme.colorScheme,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    @ReadOnlyComposable
    fun divider(): Color = resolveDivider(
        colorScheme = MaterialTheme.colorScheme,
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )
}
