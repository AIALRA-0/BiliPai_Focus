package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * An action exposed from a page-level overflow menu.
 *
 * [children] open a secondary level inside the same window popup (for example a sort order).
 */
@Immutable
data class AppWindowAction(
    val label: String,
    val onClick: (() -> Unit)? = null,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val summary: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val children: List<AppWindowAction> = emptyList(),
)

/**
 * Miuix window-level action menu used by page-level overflow buttons.
 *
 * Groups preserve the visual separation between related actions while sharing the same popup
 * implementation as Miuix settings choices.
 */
@Composable
fun AppWindowActionMenu(
    groups: List<List<AppWindowAction>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!LocalAppThemeConfig.current.nativeMiuixPopupsEnabled) {
        var expanded by remember { mutableStateOf(false) }
        var parentActions by remember { mutableStateOf(emptyList<AppWindowAction>()) }
        val visibleGroups = parentActions.lastOrNull()?.let { listOf(it.children) }
            ?: groups.filter { it.isNotEmpty() }
        Box(modifier = modifier) {
            IconButton(
                onClick = {
                    parentActions = emptyList()
                    expanded = true
                    onExpandedChange?.invoke(true)
                },
                enabled = enabled,
            ) { content() }
            AppDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    parentActions = emptyList()
                    expanded = false
                    onExpandedChange?.invoke(false)
                },
            ) {
                if (parentActions.isNotEmpty()) {
                    AppDropdownMenuItem(
                        text = { AppText("返回") },
                        onClick = { parentActions = parentActions.dropLast(1) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                visibleGroups.forEachIndexed { groupIndex, actions ->
                    if (groupIndex > 0) {
                        AppHorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    actions.forEach { action ->
                        AppDropdownMenuItem(
                            text = { AppText(action.label) },
                            leadingIcon = action.icon?.let { icon ->
                                {
                                    if (action.iconTint == null) {
                                        AppIcon(icon, contentDescription = null)
                                    } else {
                                        AppIcon(icon, contentDescription = null, tint = action.iconTint)
                                    }
                                }
                            },
                            trailingIcon = when {
                                action.children.isNotEmpty() -> { { AppText("›") } }
                                action.selected -> { { AppText("✓") } }
                                else -> null
                            },
                            enabled = action.enabled,
                            onClick = {
                                if (action.children.isNotEmpty()) {
                                    parentActions = parentActions + action
                                } else {
                                    expanded = false
                                    onExpandedChange?.invoke(false)
                                    action.onClick?.invoke()
                                }
                            },
                        )
                    }
                }
            }
        }
        return
    }

    // Own action dispatch here: the dependency's grouped window dropdown does not open
    // DropdownItem.children. Keep native rows/appearance, but explicitly handle each click.
    var expanded by remember { mutableStateOf(false) }
    var parentActions by remember { mutableStateOf(emptyList<AppWindowAction>()) }
    val visibleGroups = parentActions.lastOrNull()?.let { listOf(it.children) }
        ?: groups.filter { it.isNotEmpty() }

    Box(modifier = modifier) {
        AppIconButton(
            enabled = enabled && groups.any { it.isNotEmpty() },
            onClick = {
                parentActions = emptyList()
                expanded = true
                onExpandedChange?.invoke(true)
            },
        ) { content() }
        WindowListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = {
                expanded = false
                onExpandedChange?.invoke(false)
            },
        ) {
            ListPopupColumn {
                if (parentActions.isNotEmpty()) {
                    AppDropdownMenuItem(
                        text = { AppText("返回") },
                        onClick = { parentActions = parentActions.dropLast(1) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                visibleGroups.forEachIndexed { groupIndex, actions ->
                    if (groupIndex > 0) {
                        AppHorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    actions.forEachIndexed { actionIndex, action ->
                        DropdownImpl(
                            item = action.toDropdownItem(),
                            optionSize = actions.size,
                            isSelected = action.selected,
                            index = actionIndex,
                            enabled = action.enabled,
                            hasSubmenu = action.children.isNotEmpty(),
                            isFirst = parentActions.isEmpty() && groupIndex == 0 && actionIndex == 0,
                            isLast = groupIndex == visibleGroups.lastIndex && actionIndex == actions.lastIndex,
                            onSelectedIndexChange = {
                                if (expanded && action.enabled) {
                                    if (action.children.isNotEmpty()) {
                                        parentActions = parentActions + action
                                    } else {
                                        // Close before invoking: one selection must never toggle twice.
                                        expanded = false
                                        onExpandedChange?.invoke(false)
                                        action.onClick?.invoke()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun AppWindowAction.toDropdownItem(): DropdownItem = DropdownItem(
    text = label,
    enabled = enabled,
    selected = selected,
    icon = icon?.let { imageVector ->
        { modifier ->
            if (iconTint == null) {
                AppIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = modifier,
                )
            } else {
                AppIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = modifier,
                )
            }
        }
    },
    summary = summary,
    children = children.takeIf { it.isNotEmpty() }?.map(AppWindowAction::toDropdownItem),
)
