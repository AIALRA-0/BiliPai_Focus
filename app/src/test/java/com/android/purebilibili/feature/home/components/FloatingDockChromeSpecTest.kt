package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingDockChromeSpecTest {
    @Test
    fun liquidDockGeometryRetainsItsCurrentMeasurements() {
        assertEquals(2.dp, FloatingDockChromeSpec.SpecularInnerBlurRadius)
        assertEquals(10.dp, FloatingDockChromeSpec.DockShadowRadius)
        assertEquals(16.dp, FloatingDockChromeSpec.PressScaleTravel)
        assertEquals(25.dp, FloatingDockChromeSpec.ShellBlurRadius)
        assertEquals(10.dp, FloatingDockChromeSpec.IndicatorRefractionHeight)
        assertEquals(14.dp, FloatingDockChromeSpec.IndicatorRefractionAmount)
        assertEquals(8.dp, FloatingDockChromeSpec.PressInnerShadowRadius)
        assertEquals(3.dp, FloatingDockChromeSpec.SolidFallbackShadowRadius)
    }
}
