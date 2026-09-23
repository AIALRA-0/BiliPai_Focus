package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp

/** Pixel-preserving geometry for the liquid dock's light and refraction effects. */
internal object FloatingDockChromeSpec {
    val SpecularInnerBlurRadius = 2.dp
    val DockShadowRadius = 10.dp
    val PressScaleTravel = 16.dp
    val ShellBlurRadius = 25.dp
    val IndicatorRefractionHeight = 10.dp
    val IndicatorRefractionAmount = 14.dp
    val PressInnerShadowRadius = 8.dp
    val SolidFallbackShadowRadius = 3.dp
}
