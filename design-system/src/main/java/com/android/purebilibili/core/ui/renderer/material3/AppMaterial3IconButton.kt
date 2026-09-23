package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.AppIconButtonColors
import com.android.purebilibili.core.ui.components.AppIconButtonVariant
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3IconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: AppIconButtonColors?,
    interactionSource: MutableInteractionSource?,
    variant: AppIconButtonVariant,
    content: @Composable () -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val nativeShape = when (variant) {
        AppIconButtonVariant.Standard -> IconButtonDefaults.standardShape
        AppIconButtonVariant.Filled -> IconButtonDefaults.filledShape
    }
    val defaultColors = when (variant) {
        AppIconButtonVariant.Standard -> IconButtonDefaults.iconButtonColors()
        AppIconButtonVariant.Filled -> IconButtonDefaults.filledIconButtonColors()
    }
    val nativeColors = colors?.let {
        defaultColors.copy(
            containerColor = it.containerColor,
            contentColor = it.contentColor,
            disabledContainerColor = it.disabledContainerColor,
            disabledContentColor = it.disabledContentColor,
        )
    } ?: defaultColors
    val nativeModifier = modifier.appDesktopInteractionVisuals(
        interactionSource = resolvedInteractionSource,
        enabled = enabled,
        shape = nativeShape,
    )

    when (variant) {
        AppIconButtonVariant.Standard -> IconButton(
            onClick = onClick,
            modifier = nativeModifier,
            enabled = enabled,
            colors = nativeColors,
            interactionSource = resolvedInteractionSource,
            shape = nativeShape,
            content = content,
        )
        AppIconButtonVariant.Filled -> FilledIconButton(
            onClick = onClick,
            modifier = nativeModifier,
            enabled = enabled,
            shape = nativeShape,
            colors = nativeColors,
            interactionSource = resolvedInteractionSource,
            content = content,
        )
    }
}
