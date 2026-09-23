package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.AppSliderColors
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
    colors: AppSliderColors?,
    interactionSource: MutableInteractionSource?,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val nativeColors = colors?.let {
        SliderDefaults.colors(
            thumbColor = it.thumbColor,
            activeTrackColor = it.activeTrackColor,
            inactiveTrackColor = it.inactiveTrackColor,
        )
    } ?: SliderDefaults.colors()
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.appDesktopInteractionVisuals(resolvedInteractionSource, enabled),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = nativeColors,
        interactionSource = resolvedInteractionSource,
    )
}
