package com.android.purebilibili.feature.search

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.resolveFilledSelectionAccentColors

internal data class SearchSelectionChipColors(
    val backgroundColor: Color,
    val textColor: Color
)

internal fun resolveSearchSelectionChipColors(
    isSelected: Boolean,
    colorScheme: ColorScheme,
    unselectedAlpha: Float = 0.6f
): SearchSelectionChipColors {
    if (!isSelected) {
        return SearchSelectionChipColors(
            backgroundColor = colorScheme.surfaceVariant.copy(alpha = unselectedAlpha),
            textColor = colorScheme.onSurfaceVariant
        )
    }

    val selectedColors = resolveFilledSelectionAccentColors(colorScheme)

    return SearchSelectionChipColors(
        backgroundColor = selectedColors.backgroundColor,
        textColor = selectedColors.contentColor
    )
}
