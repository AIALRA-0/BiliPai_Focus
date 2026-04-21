package com.android.purebilibili.feature.space

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceSelectionChipColorPolicyTest {

    @Test
    fun `selected chip falls back to container when dark theme primary is near white`() {
        val scheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF2A2A2A),
            onPrimaryContainer = Color(0xFFF2F2F2),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = Color(0xFFBEBEBE)
        )

        val colors = resolveSpaceSelectionChipColors(
            isSelected = true,
            colorScheme = scheme
        )

        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.textColor)
    }

    @Test
    fun `selected chip keeps primary when contrast is healthy`() {
        val scheme = darkColorScheme(
            primary = Color(0xFF0057D8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0F2942),
            onPrimaryContainer = Color(0xFFD6E9FF),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = Color(0xFFBEBEBE)
        )

        val colors = resolveSpaceSelectionChipColors(
            isSelected = true,
            colorScheme = scheme
        )

        assertEquals(scheme.primary, colors.backgroundColor)
        assertEquals(scheme.onPrimary, colors.textColor)
    }

    @Test
    fun `unselected chip uses toned surface variant`() {
        val scheme = darkColorScheme()

        val colors = resolveSpaceSelectionChipColors(
            isSelected = false,
            colorScheme = scheme,
            unselectedAlpha = 0.7f
        )

        assertEquals(scheme.surfaceVariant.copy(alpha = 0.7f), colors.backgroundColor)
        assertEquals(scheme.onSurfaceVariant, colors.textColor)
    }

    @Test
    fun `followed button uses theme aware secondary container`() {
        val scheme = darkColorScheme(
            secondaryContainer = Color(0xFF3D2F14),
            onSecondaryContainer = Color(0xFFFFDFA0),
            primary = Color(0xFF9FCBFF),
            onPrimary = Color(0xFF003258)
        )

        val followed = resolveSpaceFollowButtonColors(isFollowed = true, colorScheme = scheme)
        val unfollowed = resolveSpaceFollowButtonColors(isFollowed = false, colorScheme = scheme)

        assertEquals(scheme.secondaryContainer, followed.backgroundColor)
        assertEquals(scheme.onSecondaryContainer, followed.textColor)
        assertEquals(scheme.primary, unfollowed.backgroundColor)
        assertEquals(scheme.onPrimary, unfollowed.textColor)
    }

    @Test
    fun `official tag uses theme aware tertiary container`() {
        val scheme = darkColorScheme(
            tertiaryContainer = Color(0xFF4A3420),
            onTertiaryContainer = Color(0xFFFFDCC4)
        )

        val colors = resolveSpaceOfficialTagColors(scheme)

        assertEquals(scheme.tertiaryContainer, colors.backgroundColor)
        assertEquals(scheme.onTertiaryContainer, colors.textColor)
    }
}
