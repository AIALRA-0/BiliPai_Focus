package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier.appDesktopInteractionVisuals(
            resolvedInteractionSource,
            enabled && onClick != null,
        ),
        enabled = enabled,
        colors = RadioButtonDefaults.colors(),
        interactionSource = resolvedInteractionSource,
    )
}
