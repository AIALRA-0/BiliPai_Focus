package com.android.purebilibili.feature.space

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors

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

    val selectedColors = resolveAdaptivePrimaryAccentColors(colorScheme)

    return SpaceSelectionChipColors(
        backgroundColor = selectedColors.backgroundColor,
        textColor = selectedColors.contentColor
    )
}

internal fun resolveSpaceFollowButtonColors(
    isFollowed: Boolean,
    colorScheme: ColorScheme
): SpaceSelectionChipColors {
    return if (isFollowed) {
        SpaceSelectionChipColors(
            backgroundColor = colorScheme.secondaryContainer,
            textColor = colorScheme.onSecondaryContainer
        )
    } else {
        val selectedColors = resolveAdaptivePrimaryAccentColors(colorScheme)
        SpaceSelectionChipColors(
            backgroundColor = selectedColors.backgroundColor,
            textColor = selectedColors.contentColor
        )
    }
}

internal fun resolveSpaceOfficialTagColors(
    colorScheme: ColorScheme
): SpaceSelectionChipColors {
    return SpaceSelectionChipColors(
        backgroundColor = colorScheme.tertiaryContainer,
        textColor = colorScheme.onTertiaryContainer
    )
}
