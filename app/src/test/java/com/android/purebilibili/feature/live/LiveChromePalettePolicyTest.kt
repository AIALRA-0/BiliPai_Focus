package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveChromePalettePolicyTest {

    @Test
    fun `live chrome palette follows material theme colors`() {
        val palette = resolveLiveChromePalette(
            isDark = false,
            primary = Color(0xFF006B5F),
            onPrimary = Color.White,
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFFFFBFE),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            surfaceVariant = Color(0xFFE7E0EC),
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
            outlineVariant = Color(0xFFCAC4D0)
        )

        assertEquals(Color(0xFF006B5F), palette.accent)
        assertEquals(Color(0xFF006B5F), palette.accentStrong)
        assertEquals(Color(0xFFFFFBFE), palette.backgroundTop)
        assertEquals(Color(0xFFF7F2FA), palette.backgroundBottom)
        assertEquals(Color(0xFF1C1B1F), palette.primaryText)
        assertEquals(Color(0xFF49454F), palette.secondaryText)
        assertEquals(Color.White, palette.onAccent)
    }
}
