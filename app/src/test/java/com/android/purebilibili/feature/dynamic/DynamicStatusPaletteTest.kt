package com.android.purebilibili.feature.dynamic

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicStatusPaletteTest {
    @Test
    fun likedColor_followsThemeTertiary() {
        val tertiary = Color(0xFF6750A4)
        val scheme = androidx.compose.material3.lightColorScheme(tertiary = tertiary)
        assertEquals(tertiary, resolveDynamicLikedColor(scheme))
    }
}
