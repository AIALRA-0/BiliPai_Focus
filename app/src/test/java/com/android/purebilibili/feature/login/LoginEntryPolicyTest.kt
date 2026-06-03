package com.android.purebilibili.feature.login

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginEntryPolicyTest {

    @Test
    fun `login methods should only expose qr code entry`() {
        assertEquals(listOf(LoginMethod.QR_CODE), resolveAvailableLoginMethods())
    }

    @Test
    fun `qr login reason should explain why scan is required`() {
        val reason = resolveQrLoginReason()

        assertTrue(reason.contains("当前仅保留扫码登录"))
        assertTrue(reason.contains("高画质"))
    }

    @Test
    fun `light login palette derives interface colors from material scheme`() {
        val scheme = lightColorScheme(
            primary = Color(0xFF1144AA),
            onPrimary = Color(0xFFF8FAFF),
            primaryContainer = Color(0xFFDDE8FF),
            secondary = Color(0xFF336655),
            tertiary = Color(0xFF774488),
            error = Color(0xFFB00020),
            surface = Color(0xFFFAFCFF),
            onSurface = Color(0xFF101820),
            surfaceVariant = Color(0xFFE7ECF4),
            onSurfaceVariant = Color(0xFF526070),
            background = Color(0xFFF4F8FF),
            outlineVariant = Color(0xFFC9D0DC)
        )

        val palette = resolveLoginPalette(scheme, darkTheme = false)

        assertEquals(scheme.background, palette.bgMid)
        assertEquals(scheme.surface, palette.panelFill)
        assertEquals(scheme.onSurface, palette.primaryText)
        assertEquals(scheme.onSurfaceVariant, palette.secondaryText)
        assertEquals(scheme.surfaceVariant, palette.inputFill)
        assertEquals(scheme.primary, palette.buttonGradientStart)
        assertEquals(scheme.tertiary, palette.buttonGradientEnd)
        assertEquals(scheme.error, palette.error)
    }

    @Test
    fun `dark login palette derives interface colors from material scheme`() {
        val scheme = darkColorScheme(
            primary = Color(0xFF8BB6FF),
            onPrimary = Color(0xFF08234F),
            primaryContainer = Color(0xFF163B70),
            secondary = Color(0xFF8ED8C4),
            tertiary = Color(0xFFE2B4F0),
            error = Color(0xFFFFB4AB),
            surface = Color(0xFF111820),
            onSurface = Color(0xFFEAF0F8),
            surfaceVariant = Color(0xFF29313A),
            onSurfaceVariant = Color(0xFFC4CAD4),
            background = Color(0xFF0A1018),
            outlineVariant = Color(0xFF404955)
        )

        val palette = resolveLoginPalette(scheme, darkTheme = true)

        assertEquals(scheme.background, palette.bgTop)
        assertEquals(scheme.surface, palette.panelFill)
        assertEquals(scheme.onSurface, palette.primaryText)
        assertEquals(scheme.onSurfaceVariant, palette.secondaryText)
        assertEquals(scheme.surfaceVariant, palette.inputFill)
        assertEquals(scheme.primary, palette.buttonGradientStart)
        assertEquals(scheme.tertiary, palette.buttonGradientEnd)
        assertEquals(scheme.error, palette.error)
    }
}
