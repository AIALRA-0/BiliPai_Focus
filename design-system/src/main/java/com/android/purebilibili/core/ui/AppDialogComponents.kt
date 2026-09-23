package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties

/** Style-neutral dialog entry point backed by the existing adaptive dialog renderer. */
@Composable
fun AppAlertDialog(
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
) = AdaptiveAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    icon = icon,
    title = title,
    text = text,
    confirmButton = confirmButton,
    dismissButton = dismissButton,
    shape = shape,
    containerColor = containerColor,
    tonalElevation = tonalElevation,
    properties = properties,
    contentLayout = contentLayout,
)

/** Style-neutral action slot for [AppAlertDialog]. */
@Composable
fun AppDialogAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = AdaptiveDialogAction(onClick = onClick, modifier = modifier, content = content)
