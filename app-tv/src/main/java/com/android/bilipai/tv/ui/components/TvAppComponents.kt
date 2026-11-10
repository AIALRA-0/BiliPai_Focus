@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.android.bilipai.tv.ui.LocalTvReturnTarget
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import com.android.bilipai.tv.ui.LocalTvInteractive
import com.android.bilipai.tv.ui.TvMotion
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.ui.LocalTvReduceMotion
import com.android.bilipai.tv.ui.TvUiTokens
import com.android.purebilibili.core.ui.ContainerLevel

/** The caller owns content, placement, and focus restoration. */
@Composable
internal fun TvAppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val interactive = LocalTvInteractive.current
    val reduce = LocalTvReduceMotion.current
    val scale = animateFloatAsState(if (focused && !reduce) TvUiTokens.focusedCardScale else 1f,
        tween(if (reduce) 0 else TvMotion.focusMs, easing = AppMotionEasing.Continuity), label = "tv-card-focus")
    val shape = TvUiTokens.shape(ContainerLevel.Card)
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = { if (interactive) onClick() },
        modifier = modifier.then(rememberReturnFocus(source, interactive)).focusProperties { canFocus = interactive }.graphicsLayer {
            scaleX = scale.value; scaleY = scale.value
        },
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.surfaceVariant,
            focusedContentColor = colors.onSurface,
            pressedContainerColor = colors.primary.copy(alpha = 0.22f),
            pressedContentColor = colors.onSurface,
        ),
        scale = CardDefaults.scale(
            focusedScale = 1f, pressedScale = 1f
        ),
        border = CardDefaults.border(focusedBorder = Border(
            border = BorderStroke(TvUiTokens.focusBorderWidth, MaterialTheme.colorScheme.primary),
            shape = shape,
        )),
        interactionSource = source,
        content = content,
    )
}

@Composable
internal fun TvAppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactive = LocalTvInteractive.current
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = { if (interactive && !isLoading) onClick() },
        modifier = modifier.then(rememberReturnFocus(source, interactive)).heightIn(min = TvUiTokens.minimumButtonHeight),
        enabled = enabled && interactive,
        interactionSource = source,
        shape = ButtonDefaults.shape(shape = TvUiTokens.buttonShape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        // Retain the focused target while its action is in progress.
        if (isLoading) Text("处理中…") else content()
    }
}

@Composable
internal fun TvNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactive = LocalTvInteractive.current
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = { if (interactive) onClick() },
        enabled = interactive,
        interactionSource = source,
        modifier = modifier.then(rememberReturnFocus(source, interactive))
            .heightIn(min = TvUiTokens.minimumButtonHeight)
            .semantics { this.selected = selected },
        shape = ButtonDefaults.shape(shape = TvUiTokens.buttonShape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) colors.primary.copy(alpha = 0.18f) else colors.surfaceVariant,
            contentColor = if (selected) colors.primary else colors.onSurface,
            focusedContainerColor = colors.surfaceVariant,
            focusedContentColor = colors.onSurface,
        ),
        border = ButtonDefaults.border(focusedBorder = Border(
            border = BorderStroke(TvUiTokens.focusBorderWidth, colors.primary),
            shape = TvUiTokens.buttonShape,
        )),
        content = content,
    )
}

/** The rail returns to the actual triggering card/button, including Banner and continue-watching. */
@Composable
private fun rememberReturnFocus(source: MutableInteractionSource, interactive: Boolean): Modifier {
    val requester = remember { FocusRequester() }
    val target = LocalTvReturnTarget.current
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused, interactive, target) { if (focused && interactive) target?.requester = requester }
    return Modifier.focusRequester(requester)
}

/**
 * 官方 FilterChip 适配：筛选行（热门子分类、分区、历史类型、已看完）统一使用官方选中语义，
 * 形态与焦点边框沿用 TV tokens；选中态与焦点态分别表达。
 */
@Composable
internal fun TvFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactive = LocalTvInteractive.current
    val source = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val shape = TvUiTokens.buttonShape
    FilterChip(
        selected = selected,
        onClick = { if (interactive) onClick() },
        enabled = interactive,
        interactionSource = source,
        modifier = modifier.then(rememberReturnFocus(source, interactive))
            .heightIn(min = TvUiTokens.minimumButtonHeight)
            .semantics { this.selected = selected },
        shape = FilterChipDefaults.shape(shape = shape),
        colors = FilterChipDefaults.colors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.surfaceVariant,
            focusedContentColor = colors.onSurface,
            selectedContainerColor = colors.primary.copy(alpha = 0.18f),
            selectedContentColor = colors.primary,
            focusedSelectedContainerColor = colors.primary.copy(alpha = 0.28f),
            focusedSelectedContentColor = colors.primary,
        ),
        border = FilterChipDefaults.border(focusedBorder = Border(
            border = BorderStroke(TvUiTokens.focusBorderWidth, colors.primary),
            shape = shape,
        )),
        content = content,
    )
}
