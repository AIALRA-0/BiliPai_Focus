package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.TextPrimaryDark

internal data class AppUpdateDialogTextColors(
    val titleColor: Color,
    val currentVersionColor: Color,
    val releaseNotesColor: Color
)

internal fun resolveAppUpdateDialogTextColors(isDarkTheme: Boolean): AppUpdateDialogTextColors {
    if (!isDarkTheme) {
        return AppUpdateDialogTextColors(
            titleColor = Color.Unspecified,
            currentVersionColor = Color.Unspecified,
            releaseNotesColor = Color.Unspecified
        )
    }

    val highContrastText = TextPrimaryDark
    return AppUpdateDialogTextColors(
        titleColor = highContrastText,
        currentVersionColor = highContrastText,
        releaseNotesColor = highContrastText
    )
}

internal fun resolveAppUpdateScrollableContentMaxHeight(
    screenHeightDp: Int,
    heightFraction: Float,
    minHeightDp: Int,
    maxHeightDp: Int,
): Int {
    return (screenHeightDp * heightFraction)
        .toInt()
        .coerceIn(minHeightDp, maxHeightDp)
}
