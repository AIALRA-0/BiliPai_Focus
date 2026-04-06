package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MiuixThemeBridgePolicyTest {

    @Test
    fun `material bridge preserves primary and surface roles from miuix colors`() {
        val materialScheme = resolveMaterialColorSchemeFromMiuixBridge(
            bridge = MiuixMaterialBridge(
                primary = Color(0xFF3482FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE1ECFF),
                onPrimaryContainer = Color(0xFF001C3A),
                secondary = Color(0xFF5A5F71),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFDEE3F9),
                onSecondaryContainer = Color(0xFF171B2C),
                tertiary = Color(0xFF75546F),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFD7F5),
                onTertiaryContainer = Color(0xFF2C1229),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                background = Color(0xFFF8F9FF),
                onBackground = Color(0xFF191C20),
                surface = Color(0xFFF8F9FF),
                onSurface = Color(0xFF191C20),
                surfaceVariant = Color(0xFFE0E2EC),
                onSurfaceVariant = Color(0xFF44474E),
                surfaceContainer = Color(0xFFECEEF4),
                surfaceContainerHigh = Color(0xFFE6E8EE),
                outline = Color(0xFF74777F),
                outlineVariant = Color(0xFFC4C6D0)
            ),
            amoledDarkTheme = false
        )

        assertEquals(Color(0xFF3482FF), materialScheme.primary)
        assertEquals(Color(0xFFF8F9FF), materialScheme.background)
        assertEquals(Color(0xFFF8F9FF), materialScheme.surface)
        assertEquals(Color(0xFFECEEF4), materialScheme.surfaceContainer)
        assertEquals(Color(0xFFE6E8EE), materialScheme.surfaceContainerHigh)
    }
}
