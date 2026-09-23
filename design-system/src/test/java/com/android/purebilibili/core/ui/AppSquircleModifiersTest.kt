package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSquircleModifiersTest {

    @Test
    fun squircleBackground_appliesOnlyOnMiuixStyle() {
        assertTrue(shouldApplyMiuixSquircleBackground(AppUiStyle.MIUIX))
        assertFalse(shouldApplyMiuixSquircleBackground(AppUiStyle.MATERIAL3))
    }
}
