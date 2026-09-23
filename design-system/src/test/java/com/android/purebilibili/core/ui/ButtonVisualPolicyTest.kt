package com.android.purebilibili.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ButtonVisualPolicyTest {

    @Test
    fun `filled button uses primary on dark theme and primaryContainer on light theme`() {
        val light = lightColorScheme(primary = Color(0xFF0F2A6A), primaryContainer = Color(0xFFD9E2FF))
        val dark = darkColorScheme(primary = Color(0xFFAEC6FF), primaryContainer = Color(0xFF1E3A8A))

        assertFalse(isColorSchemeDark(light))
        assertTrue(isColorSchemeDark(dark))

        assertEquals(light.primaryContainer, resolveFilledButtonContainerColor(light))
        assertEquals(light.onPrimaryContainer, resolveFilledButtonContentColor(light))
        assertEquals(dark.primary, resolveFilledButtonContainerColor(dark))
        assertEquals(dark.onPrimary, resolveFilledButtonContentColor(dark))
    }
}
