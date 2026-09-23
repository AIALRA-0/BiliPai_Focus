package com.android.purebilibili.core.ui

import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppThemeConfigTest {

    @Test
    fun `defaults keep standalone UI hosts functional`() {
        val config = AppThemeConfig()

        assertEquals(BlurIntensity.THIN, config.blurIntensity)
        assertTrue(config.headerBlurEnabled)
        assertFalse(config.bottomBarBlurEnabled)
        assertTrue(config.hapticFeedbackEnabled)
        assertFalse(config.globalTextTapCopyEnabled)
        assertTrue(config.uiEntranceAnimationEnabled)
        assertTrue(config.runtimeVisualGuardEnabled)
        assertFalse(config.liquidGlassEnabled)
        assertTrue(config.progressiveTopFadeEnabled)
    }

    @Test
    fun `non glass design is scoped to Miuix without changing blur preferences`() {
        for (style in AppUiStyle.entries) {
            for (glass in listOf(false, true)) {
                assertEquals(style == AppUiStyle.MIUIX && !glass, isMiuixNonGlassEnabled(style, glass))
            }
        }
        val config = AppThemeConfig(liquidGlassEnabled = false, bottomBarBlurEnabled = true)
        assertTrue(config.headerBlurEnabled)
        assertTrue(config.bottomBarBlurEnabled)
    }

    @Test
    fun `ordinary blur requires an available backdrop and never enables glass`() {
        assertEquals(AppChromeMaterial.SOLID, resolveAppChromeMaterial(false, false, true))
        assertEquals(AppChromeMaterial.SOLID, resolveAppChromeMaterial(false, true, false))
        assertEquals(AppChromeMaterial.BLUR, resolveAppChromeMaterial(false, true, true))
        assertEquals(AppChromeMaterial.LIQUID_GLASS, resolveAppChromeMaterial(true, false, true))
    }
}
