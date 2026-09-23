package com.android.purebilibili.feature.live

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.resolveFilledSelectionAccentColors

internal data class LiveListTabColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color
)

/**
 * Live list tab colors from the active theme [ColorScheme] only —
 * no hard-coded surface / accent hex values.
 */
internal fun resolveLiveListTabColors(
    colorScheme: ColorScheme,
): LiveListTabColors {
    val selected = resolveFilledSelectionAccentColors(colorScheme)
    return LiveListTabColors(
        selectedContainerColor = selected.backgroundColor,
        selectedContentColor = selected.contentColor,
        unselectedContainerColor = colorScheme.surfaceVariant,
        unselectedContentColor = colorScheme.onSurfaceVariant,
    )
}
