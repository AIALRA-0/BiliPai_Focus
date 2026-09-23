package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    thumbContent: (@Composable () -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    showThumbIcon: Boolean,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val interactive = enabled && onCheckedChange != null
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.appDesktopInteractionVisuals(resolvedInteractionSource, interactive),
        thumbContent = thumbContent ?: if (showThumbIcon) {
            {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
        enabled = enabled,
        colors = SwitchDefaults.colors(),
        interactionSource = resolvedInteractionSource,
    )
}
