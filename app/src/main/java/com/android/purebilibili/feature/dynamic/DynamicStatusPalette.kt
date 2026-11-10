package com.android.purebilibili.feature.dynamic

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.Color

internal fun resolveDynamicLikedColor(colorScheme: ColorScheme): Color = colorScheme.tertiary

internal object DynamicStatusPalette {
    val MediaContent = Color.White

    @Composable
    fun liked(): Color = if (LocalAppUiStyle.current == AppUiStyle.MIUIX) {
        MiuixTheme.colorScheme.primary
    } else {
        resolveDynamicLikedColor(MaterialTheme.colorScheme)
    }
}
