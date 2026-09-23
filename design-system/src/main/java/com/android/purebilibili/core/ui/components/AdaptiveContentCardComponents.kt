package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Content card that follows the active UI style:
 * - Material 3 → [Card] with the theme's large content-container shape
 * - Miuix → [MiuixCard] with native corner radius
 *
 * Prefer this over hand-rolled [Surface] + [RoundedCornerShape] for video/detail
 * summary panels so radius and container treatment stay theme-native.
 */
@Composable
fun AppContentCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalAppUiStyle.current) {
        AppUiStyle.MIUIX -> {
            if (onClick != null) {
                MiuixCard(
                    modifier = modifier,
                    cornerRadius = MiuixCardDefaults.CornerRadius,
                    insideMargin = contentPadding,
                    colors = MiuixCardDefaults.defaultColors(
                        color = containerColor,
                        contentColor = contentColor,
                    ),
                    pressFeedbackType = PressFeedbackType.Sink,
                    onClick = onClick,
                    content = content,
                )
            } else {
                MiuixCard(
                    modifier = modifier,
                    cornerRadius = MiuixCardDefaults.CornerRadius,
                    insideMargin = contentPadding,
                    colors = MiuixCardDefaults.defaultColors(
                        color = containerColor,
                        contentColor = contentColor,
                    ),
                    content = content,
                )
            }
        }
        AppUiStyle.MATERIAL3 -> {
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(contentPadding),
                        content = content,
                    )
                }
            } else {
                Card(
                    modifier = modifier,
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(contentPadding),
                        content = content,
                    )
                }
            }
        }
    }
}

/**
 * Compact tag / keyword chip:
 * - Material 3 → [AssistChip]
 * - Miuix → themed [Surface] with [ContainerLevel.Chip] / pill-scale corners
 */
@Composable
fun AppTagChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> {
            AssistChip(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                label = {
                    AppText(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colorScheme.surfaceContainerHighest,
                    labelColor = colorScheme.onSurfaceVariant,
                ),
                border = AssistChipDefaults.assistChipBorder(enabled = enabled),
            )
        }
        AppUiStyle.MIUIX -> {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = AppShapes.container(ContainerLevel.Chip),
                color = colorScheme.surfaceContainerHighest,
                contentColor = colorScheme.onSurfaceVariant,
            ) {
                AppText(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/**
 * Small badge pill (non-clickable) for status labels on video detail.
 */
@Composable
fun AppStatusBadge(
    label: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (emphasized) {
        colorScheme.primaryContainer
    } else {
        colorScheme.surfaceContainerHighest
    }
    val contentColor = if (emphasized) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = when (LocalAppUiStyle.current) {
            AppUiStyle.MATERIAL3 -> MaterialTheme.shapes.small
            AppUiStyle.MIUIX -> AppShapes.container(ContainerLevel.Tag)
        },
        color = containerColor,
        contentColor = contentColor,
    ) {
        AppText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
