package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveScaffoldPolicyTest {

    @Test
    fun miuixStyle_routesToMiuixScaffoldWithPopupHost() {
        assertEquals(
            AdaptiveScaffoldRenderer.MIUIX_SCAFFOLD_WITH_POPUP_HOST,
            resolveAdaptiveScaffoldRenderer(AppUiStyle.MIUIX)
        )
        assertTrue(
            shouldMountMiuixPopupHostOnAdaptiveScaffold(AppUiStyle.MIUIX)
        )
    }

    @Test
    fun material3Style_routesToMaterial3Scaffold() {
        assertEquals(
            AdaptiveScaffoldRenderer.MATERIAL3_SCAFFOLD,
            resolveAdaptiveScaffoldRenderer(AppUiStyle.MATERIAL3)
        )
        assertFalse(
            shouldMountMiuixPopupHostOnAdaptiveScaffold(AppUiStyle.MATERIAL3)
        )
    }
}
