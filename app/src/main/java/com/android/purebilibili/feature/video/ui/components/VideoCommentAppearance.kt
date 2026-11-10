package com.android.purebilibili.feature.video.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

internal enum class VideoCommentTextRole { AUTHOR, METADATA, BODY, ACTION, ACTION_COUNT, SUB_REPLY }

internal fun resolveVideoCommentFontSize(typography: Typography, role: VideoCommentTextRole): TextUnit =
    when (role) {
        VideoCommentTextRole.AUTHOR -> typography.bodySmall.fontSize
        // 评论正文与楼中楼都是主要阅读内容，跟随页面正文的字阶。
        VideoCommentTextRole.SUB_REPLY,
        VideoCommentTextRole.BODY -> typography.bodyMedium.fontSize
        VideoCommentTextRole.METADATA, VideoCommentTextRole.ACTION_COUNT -> typography.labelSmall.fontSize
        VideoCommentTextRole.ACTION -> typography.labelMedium.fontSize
    }

internal object VideoCommentTypographyTokens {
    val author: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.AUTHOR)
    val metadata: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.METADATA)
    val body: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.BODY)
    val action: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.ACTION)
    val actionCount: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.ACTION_COUNT)
    val subReply: TextUnit
        @Composable @ReadOnlyComposable
        get() = resolveVideoCommentFontSize(MaterialTheme.typography, VideoCommentTextRole.SUB_REPLY)
    val metadataLineHeight: TextUnit
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.labelSmall.lineHeight
}

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
