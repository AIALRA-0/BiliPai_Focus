package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox as MiuixTooltipBox

/**
 * Long-press / hover tooltip bridge.
 *
 * MIUIX uses the official [MiuixTooltipBox] plain-text convenience API.
 * Material 3 uses the official [TooltipBox] with [PlainTooltip].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePlainTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    when (rememberPresetPrimitiveRenderer()) {
        PresetPrimitiveRenderer.MIUIX_BRIDGED -> {
            MiuixTooltipBox(
                text = text,
                modifier = modifier,
                enabled = enabled && text.isNotBlank(),
                positioning = TooltipAnchorPosition.Below,
                content = content
            )
        }
        PresetPrimitiveRenderer.MATERIAL3 -> {
            if (text.isNotBlank() && enabled) {
                val tooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(text = text)
                        }
                    },
                    state = tooltipState,
                    modifier = modifier,
                    focusable = false,
                    enableUserInput = enabled,
                    content = content
                )
            } else {
                Box(modifier = modifier) {
                    content()
                }
            }
        }
    }
}
