package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Style-neutral preference entry points.
 *
 * The first migration phase delegates to the proven adaptive renderers so callers can move
 * away from historical IOS* names without changing layout, interaction, or persistence.
 */
@Composable
fun AppPreferenceSectionTitle(title: String) = AdaptivePreferenceSectionTitleRenderer(title)

enum class AppPreferenceGroupPresentation {
    CARD,
    FLAT,
}

@Composable
fun AppPreferenceGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape? = null,
    border: BorderStroke? = null,
    presentation: AppPreferenceGroupPresentation = AppPreferenceGroupPresentation.CARD,
    content: @Composable ColumnScope.() -> Unit,
) = AdaptivePreferenceGroupRenderer(
    modifier = modifier,
    containerColor = containerColor,
    shape = shape,
    border = border,
    presentation = presentation,
    content = content,
)

@Composable
fun AppSwitchPreference(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) = AdaptiveSwitchPreferenceContent(
    icon = icon,
    title = title,
    subtitle = subtitle,
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
    iconTint = iconTint,
    textColor = textColor,
    subtitleColor = subtitleColor,
)

@Composable
fun AppSliderPreference(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.primary,
) = AdaptiveSliderPreferenceRenderer(
    icon = icon,
    title = title,
    subtitle = subtitle,
    value = value,
    onValueChange = onValueChange,
    valueRange = valueRange,
    steps = steps,
    valueLabel = valueLabel,
    iconTint = iconTint,
    textColor = textColor,
    subtitleColor = subtitleColor,
    valueColor = valueColor,
)

@Composable
fun AppPreference(
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    copyValue: String? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevronTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    centered: Boolean = false,
    enableCopy: Boolean = false,
    onCopyRequest: ((text: String, label: String?) -> Unit)? = null,
    showChevron: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
) = AdaptivePreferenceContent(
    icon = icon,
    iconPainter = iconPainter,
    title = title,
    subtitle = subtitle,
    value = value,
    copyValue = copyValue,
    onClick = onClick,
    iconTint = iconTint,
    textColor = textColor,
    subtitleColor = subtitleColor,
    valueColor = valueColor,
    chevronTint = chevronTint,
    centered = centered,
    enableCopy = enableCopy,
    onCopyRequest = onCopyRequest,
    showChevron = showChevron,
    trailingContent = trailingContent,
)

@Composable
fun AppPreferenceDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 66.dp,
) = AdaptivePreferenceDividerRenderer(modifier = modifier, startIndent = startIndent)

@Composable
fun AppPreferenceGridItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) = AdaptivePreferenceGridItemRenderer(
    icon = icon,
    title = title,
    onClick = onClick,
    iconTint = iconTint,
    containerColor = containerColor,
    contentColor = contentColor,
    modifier = modifier,
)

enum class AppSearchFieldPresentation {
    STANDARD,
    TOP_BAR,
}

@Composable
fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: () -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") },
    presentation: AppSearchFieldPresentation = AppSearchFieldPresentation.STANDARD,
    autoFocusEnabled: Boolean = false,
    focusRequester: FocusRequester? = null,
    containerColor: Color = Color.Unspecified,
    shapeOverride: Shape? = null,
    heightOverride: Dp? = null,
    interactionSource: MutableInteractionSource? = null,
    leadingIconHorizontalOffset: Dp = 0.dp,
) = AdaptiveSearchFieldRenderer(
    query = query,
    onQueryChange = onQueryChange,
    modifier = modifier,
    placeholder = placeholder,
    containerColor = containerColor,
    shapeOverride = shapeOverride,
    heightOverride = heightOverride,
    forceExpandedInput = presentation == AppSearchFieldPresentation.TOP_BAR,
    topBarChrome = presentation == AppSearchFieldPresentation.TOP_BAR,
    onSearch = onSearch,
    onClear = onClear,
    showClearAction = presentation != AppSearchFieldPresentation.TOP_BAR,
    autoFocusEnabled = autoFocusEnabled,
    focusRequester = focusRequester,
    interactionSource = interactionSource,
    leadingIconHorizontalOffset = leadingIconHorizontalOffset,
)

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) = AdaptiveTextFieldRenderer(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    placeholder = placeholder,
    singleLine = singleLine,
    minLines = minLines,
    maxLines = maxLines,
    isError = isError,
    supportingText = supportingText,
)
