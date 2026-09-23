package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.BadgeDefaults as MiuixBadgeDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults as MiuixNavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode as MiuixNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarItemColors as MiuixNavigationBarItemColors
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults as MiuixNavigationRailDefaults
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue as MiuixNavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState as rememberMiuixNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.flow.drop

@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) = NavigationBar(
    modifier = modifier.focusGroup(),
    containerColor = containerColor,
    contentColor = contentColor,
    tonalElevation = tonalElevation,
    windowInsets = windowInsets,
    content = content,
)

@Composable
fun RowScope.AppNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable (() -> Unit))? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier.appDesktopInteractionVisuals(resolvedInteractionSource, enabled),
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = colors,
        interactionSource = resolvedInteractionSource,
    )
}

enum class AppPlatformNavigationBarDisplayMode {
    ICON_AND_TEXT,
    ICON_ONLY,
    ICON_WITH_SELECTED_LABEL,
}

private fun AppPlatformNavigationBarDisplayMode.toMiuixDisplayMode(): MiuixNavigationBarDisplayMode =
    when (this) {
        AppPlatformNavigationBarDisplayMode.ICON_AND_TEXT -> MiuixNavigationBarDisplayMode.IconAndText
        AppPlatformNavigationBarDisplayMode.ICON_ONLY -> MiuixNavigationBarDisplayMode.IconOnly
        AppPlatformNavigationBarDisplayMode.ICON_WITH_SELECTED_LABEL ->
            MiuixNavigationBarDisplayMode.IconWithSelectedLabel
    }

@Composable
fun AppPlatformNavigationBar(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    mode: AppPlatformNavigationBarDisplayMode = AppPlatformNavigationBarDisplayMode.ICON_AND_TEXT,
    content: @Composable RowScope.() -> Unit,
) = MiuixNavigationBar(
    modifier = modifier.focusGroup(),
    color = color,
    showDivider = showDivider,
    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
    mode = mode.toMiuixDisplayMode(),
    content = content,
)

@Composable
fun RowScope.AppPlatformNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MiuixNavigationBarItemColors = MiuixNavigationBarDefaults.navigationBarItemColors(),
    badge: (@Composable () -> Unit)? = null,
) = MiuixNavigationBarItem(
    selected = selected,
    onClick = onClick,
    icon = icon,
    label = label,
    modifier = modifier
        .appDesktopFocusableItemVisuals(enabled)
        .semantics {
            role = Role.Tab
            this.selected = selected
        },
    enabled = enabled,
    colors = colors,
    badge = badge,
)

@Composable
fun AppPlatformNavigationBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixBadgeDefaults.containerColor,
    contentColor: Color = MiuixBadgeDefaults.contentColor,
    content: @Composable (RowScope.() -> Unit)? = null,
) = MiuixBadge(
    modifier = modifier,
    containerColor = containerColor,
    contentColor = contentColor,
    content = content,
)

@Composable
fun AppPlatformNavigationRail(
    expanded: Boolean,
    initiallyExpanded: Boolean = expanded,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    minWidth: Dp = MiuixNavigationRailDefaults.MinWidth,
    expandedWidth: Dp = MiuixNavigationRailDefaults.ExpandedWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val latestOnExpandedChange = rememberUpdatedState(onExpandedChange)
    val state = if (expanded) {
        rememberMiuixNavigationRailState(
            if (initiallyExpanded) {
                MiuixNavigationRailValue.Expanded
            } else {
                MiuixNavigationRailValue.Collapsed
            }
        )
    } else {
        null
    }
    LaunchedEffect(state) {
        if (state != null) {
            snapshotFlow { state.currentValue }
                .drop(1)
                .collect {
                    latestOnExpandedChange.value(it == MiuixNavigationRailValue.Expanded)
                }
        }
    }
    MiuixNavigationRail(
        modifier = modifier.focusGroup(),
        state = state,
        color = color,
        showDivider = showDivider,
        minWidth = minWidth,
        expandedWidth = expandedWidth,
        content = content,
    )
}

@Composable
fun ColumnScope.AppPlatformNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = MiuixNavigationRailItem(
    selected = selected,
    onClick = onClick,
    icon = icon,
    label = label,
    modifier = modifier
        .appDesktopFocusableItemVisuals(enabled)
        .semantics {
            role = Role.Tab
            this.selected = selected
        },
    enabled = enabled,
)
