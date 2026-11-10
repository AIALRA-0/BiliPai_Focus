package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Zero is permitted while editing; the confirmation action validates the final duration. */
@Composable
fun AppDurationPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = (minutes.coerceAtLeast(0) / 60).coerceAtMost(999)
    val remainder = minutes.coerceAtLeast(0) % 60
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppNumberPicker(
            value = hours, onValueChange = { onMinutesChange(it * 60 + remainder) },
            modifier = Modifier.weight(1f), range = 0..999, label = "小时",
        )
        AppNumberPicker(
            value = remainder, onValueChange = { onMinutesChange(hours * 60 + it) },
            modifier = Modifier.weight(1f), label = "分钟",
        )
    }
}
