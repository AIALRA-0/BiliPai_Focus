package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppText

@Composable
internal fun DanmakuColorOptions(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    onCustomColor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = danmakuSendColorOptions()
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            Box(
                Modifier.size(36.dp)
                    .background(Color(option.value or 0xFF000000.toInt()), RoundedCornerShape(8.dp))
                    .border(2.dp, if (selectedColor == option.value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .selectable(selected = selectedColor == option.value, role = Role.RadioButton,
                        onClick = { onColorSelected(option.value) })
                    .semantics { contentDescription = option.label },
            )
        }
        AppFilterChip(
            selected = selectedColor >= 0 && options.none { it.value == selectedColor },
            onClick = onCustomColor,
            label = { AppText("自定义") },
        )
    }
}
