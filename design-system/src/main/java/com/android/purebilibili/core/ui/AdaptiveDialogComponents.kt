package com.android.purebilibili.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.window.WindowDialog

enum class AppAlertDialogRenderer {
    MATERIAL_ALERT,
    LOCAL_DIALOG
}

fun resolveAppAlertDialogRenderer(
    uiStyle: AppUiStyle,
    nativeMiuixPopupsEnabled: Boolean = true,
): AppAlertDialogRenderer = when (uiStyle) {
    // 设置等页对话框常在 AdaptiveScaffold 外层组合；窗口 Dialog 不依赖
    // Miuix Scaffold 的 DialogStates host，点击后状态与弹窗保持一致。
    AppUiStyle.MIUIX -> if (nativeMiuixPopupsEnabled) {
        AppAlertDialogRenderer.LOCAL_DIALOG
    } else {
        AppAlertDialogRenderer.MATERIAL_ALERT
    }
    AppUiStyle.MATERIAL3 -> AppAlertDialogRenderer.MATERIAL_ALERT
}

data class DialogActionLayoutPolicy(
    val expandToContainer: Boolean
)

/**
 * 历史 iOS 的全宽铺满操作区行为已随单向迁移删除（iOS → MIUIX 不再 expandToContainer）。
 */
fun resolveDialogActionLayoutPolicy(): DialogActionLayoutPolicy {
    return DialogActionLayoutPolicy(
        expandToContainer = false
    )
}

/**
 * Style-neutral Alert Dialog backed by the adaptive renderer.
 */
@Composable
internal fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape? = null,
    containerColor: Color? = null,
    tonalElevation: Dp? = null,
    properties: DialogProperties = DialogProperties(),
    contentLayout: AppContentDialogLayoutPolicy = resolveAppCompactContentDialogLayoutPolicy(),
) {
    val uiStyle = LocalAppUiStyle.current
    val themeConfig = LocalAppThemeConfig.current
    val renderer = resolveAppAlertDialogRenderer(
        uiStyle = uiStyle,
        nativeMiuixPopupsEnabled = themeConfig.nativeMiuixPopupsEnabled,
    )
    when (renderer) {
        AppAlertDialogRenderer.LOCAL_DIALOG -> {
            val dialogShape = shape ?: AppShapes.resolveContainerShape(
                level = ContainerLevel.Dialog,
                uiStyle = uiStyle,
                liquidGlassEnabled = false,
            )
            val dialogColor = containerColor ?: AppSurfaceTokens.cardContainer()
            val dialogBody: @Composable () -> Unit = {
                MiuixAlertDialogBody(
                    icon = icon,
                    title = title,
                    text = text,
                    confirmButton = confirmButton,
                    dismissButton = dismissButton,
                )
            }
            WindowDialog(
                show = true,
                onDismissRequest = onDismissRequest,
                maxWidth = contentLayout.maxWidthDp.dp,
            ) {
                AppPopupSurface(
                    type = AppPopupSurfaceType.DIALOG,
                    modifier = modifier
                        .fillMaxWidth()
                        .widthIn(min = contentLayout.minWidthDp.dp, max = contentLayout.maxWidthDp.dp),
                    shape = dialogShape,
                    containerColor = dialogColor,
                    tonalElevation = tonalElevation ?: 0.dp,
                ) {
                    dialogBody()
                }
            }
            return
        }
        AppAlertDialogRenderer.MATERIAL_ALERT -> {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                icon = icon,
                title = title,
                text = text,
                confirmButton = { confirmButton?.invoke() ?: Spacer(modifier = Modifier) },
                dismissButton = dismissButton,
                properties = properties,
                shape = shape ?: MaterialTheme.shapes.extraLarge,
                containerColor = containerColor ?: MaterialTheme.colorScheme.surface,
                tonalElevation = tonalElevation ?: AlertDialogDefaults.TonalElevation,
            )
            return
        }
    }
}

@Composable
private fun MiuixAlertDialogBody(
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    confirmButton: @Composable (() -> Unit)?,
    dismissButton: @Composable (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
        if (title != null) {
            Box(
                modifier = Modifier.padding(
                    top = if (icon != null) 12.dp else 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    title()
                }
            }
        }
        if (text != null) {
            Box(
                modifier = Modifier.padding(
                    top = if (title != null) 8.dp else 12.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 12.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    text()
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (dismissButton != null) {
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    dismissButton()
                }
            }
            if (confirmButton != null) {
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    confirmButton()
                }
            }
        }
    }
}

/**
 * A helper for buttons inside the adaptive alert dialog if you need absolute control,
 * basically a wrapper that removes TextButton padding issues if standard TextButton is used.
 * But actually providing TextStyle above might be enough for simple Text() children.
 * If user passes TextButton, we might need to conform it.
 */
@Composable
internal fun AdaptiveDialogAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val layoutPolicy = resolveDialogActionLayoutPolicy()
    Box(
        modifier = modifier
            .then(
                if (layoutPolicy.expandToContainer) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
