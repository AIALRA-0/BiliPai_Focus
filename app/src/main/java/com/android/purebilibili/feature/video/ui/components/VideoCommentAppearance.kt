package com.android.purebilibili.feature.video.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

internal data class VideoCommentAppearance(
    val panelColor: Color,
    val dividerColor: Color,
    val sectionDividerColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val actionTint: Color,
    val sortTint: Color,
    val auxiliaryTint: Color,
    val placeholderColor: Color,
    val composerHintBackgroundColor: Color,
    val toggleCheckedBackgroundColor: Color,
    val toggleCheckedContentColor: Color,
    val toggleUncheckedBackgroundColor: Color,
    val toggleUncheckedContentColor: Color
)

internal fun resolveVideoCommentAppearance(
    surfaceColor: Color,
    surfaceVariantColor: Color,
    surfaceContainerHighColor: Color,
    outlineVariantColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color,
    onPrimaryColor: Color
): VideoCommentAppearance {
    return VideoCommentAppearance(
        panelColor = surfaceColor,
        dividerColor = outlineVariantColor,
        sectionDividerColor = surfaceContainerHighColor,
        primaryTextColor = onSurfaceColor,
        secondaryTextColor = onSurfaceVariantColor,
        accentColor = primaryColor,
        actionTint = onSurfaceVariantColor,
        sortTint = primaryColor.copy(alpha = 0.86f),
        auxiliaryTint = primaryColor.copy(alpha = 0.72f),
        placeholderColor = surfaceVariantColor,
        composerHintBackgroundColor = surfaceVariantColor.copy(alpha = 0.40f),
        toggleCheckedBackgroundColor = primaryColor,
        toggleCheckedContentColor = onPrimaryColor,
        toggleUncheckedBackgroundColor = surfaceVariantColor.copy(alpha = 0.50f),
        toggleUncheckedContentColor = onSurfaceVariantColor
    )
}

@Composable
internal fun rememberVideoCommentAppearance(): VideoCommentAppearance {
    val colorScheme = MaterialTheme.colorScheme
    return remember(
        colorScheme.surface,
        colorScheme.surfaceVariant,
        colorScheme.surfaceContainerHigh,
        colorScheme.outlineVariant,
        colorScheme.onSurface,
        colorScheme.onSurfaceVariant,
        colorScheme.primary,
        colorScheme.onPrimary
    ) {
        resolveVideoCommentAppearance(
            surfaceColor = colorScheme.surface,
            surfaceVariantColor = colorScheme.surfaceVariant,
            surfaceContainerHighColor = colorScheme.surfaceContainerHigh,
            outlineVariantColor = colorScheme.outlineVariant,
            onSurfaceColor = colorScheme.onSurface,
            onSurfaceVariantColor = colorScheme.onSurfaceVariant,
            primaryColor = colorScheme.primary,
            onPrimaryColor = colorScheme.onPrimary
        )
    }
}
