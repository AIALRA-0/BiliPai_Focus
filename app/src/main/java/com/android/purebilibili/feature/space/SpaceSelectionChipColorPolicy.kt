package com.android.purebilibili.feature.space

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors
import com.android.purebilibili.core.theme.resolveFilledSelectionAccentColors

internal data class SpaceSelectionChipColors(
    val backgroundColor: Color,
    val textColor: Color
)

internal fun resolveSpaceSelectionChipColors(
    isSelected: Boolean,
    colorScheme: ColorScheme,
    unselectedAlpha: Float = 0.5f
): SpaceSelectionChipColors {
    if (!isSelected) {
        return SpaceSelectionChipColors(
            backgroundColor = colorScheme.surfaceVariant.copy(alpha = unselectedAlpha),
            textColor = colorScheme.onSurfaceVariant
        )
    }

    val selectedColors = resolveFilledSelectionAccentColors(colorScheme)

    return SpaceSelectionChipColors(
        backgroundColor = selectedColors.backgroundColor,
        textColor = selectedColors.contentColor
    )
}

internal fun resolveSpaceFollowButtonColors(
    isFollowed: Boolean,
    colorScheme: ColorScheme,
    isOwner: Boolean = false,
): SpaceSelectionChipColors {
    return if (isOwner || isFollowed) {
        SpaceSelectionChipColors(
            backgroundColor = colorScheme.surfaceVariant,
            textColor = colorScheme.onSurfaceVariant
        )
    } else {
        // CTA stays solid adaptive primary; chips use softer filled-selection pair.
        val selectedColors = resolveAdaptivePrimaryAccentColors(colorScheme)
        SpaceSelectionChipColors(
            backgroundColor = selectedColors.backgroundColor,
            textColor = selectedColors.contentColor
        )
    }
}
