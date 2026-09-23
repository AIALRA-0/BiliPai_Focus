package com.android.purebilibili.feature.live

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveListTabColorPolicyTest {

    @Test
    fun `resolveLiveListTabColors uses soft primaryContainer for selected tab`() {
        val scheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE8DEF8),
            onPrimaryContainer = Color(0xFF1D192B),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            surface = Color(0xFFFFFBFE),
        )

        val colors = resolveLiveListTabColors(scheme)

        assertEquals(scheme.primaryContainer, colors.selectedContainerColor)
        assertEquals(scheme.onPrimaryContainer, colors.selectedContentColor)
        assertEquals(scheme.surfaceVariant, colors.unselectedContainerColor)
        assertEquals(scheme.onSurfaceVariant, colors.unselectedContentColor)
    }

    @Test
    fun `dark scheme selected tab uses soft container fill not neon primary`() {
        val scheme = darkColorScheme(
            primary = Color(0xFFFF8A50),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF5C2E12),
            onPrimaryContainer = Color(0xFFFFDBCB),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2B2930),
            onSurfaceVariant = Color(0xFFCAC4D0),
        )

        val colors = resolveLiveListTabColors(scheme)

        assertTrue(colors.selectedContainerColor.luminance() < scheme.primary.luminance())
        assertTrue(colors.selectedContentColor.luminance() > 0.5f)
    }
}
