package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTopChromePolicyTest {
    @Test
    fun twoValueStylesMapToSemanticTopChromeTreatments() {
        val miuix = resolveAppTopChromePolicy(AppUiStyle.MIUIX)
        val material3 = resolveAppTopChromePolicy(AppUiStyle.MATERIAL3)

        assertEquals(AppTopTabPresentation.MATERIAL_UNDERLINE, miuix.tabPresentation)
        assertEquals(AppSemanticIconFamily.MATERIAL, miuix.iconFamily)
        assertEquals(AppTopTabPresentation.MATERIAL_UNDERLINE, material3.tabPresentation)
        assertEquals(AppSemanticIconFamily.MATERIAL, material3.iconFamily)
    }
}
