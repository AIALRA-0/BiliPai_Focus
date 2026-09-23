package com.android.purebilibili.feature.dynamic

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal fun resolveDynamicLikedColor(colorScheme: ColorScheme): Color = colorScheme.tertiary

internal object DynamicStatusPalette {
    @Composable
    fun liked(): Color = resolveDynamicLikedColor(MaterialTheme.colorScheme)
}
