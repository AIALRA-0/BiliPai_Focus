package com.android.purebilibili.core.ui

import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.LocalTextStyle as MaterialLocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Theme-value bridge only; visible components remain isolated in renderer packages. */
@Composable
@ReadOnlyComposable
internal fun currentAppTextStyle(): TextStyle = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> MaterialLocalTextStyle.current
    AppUiStyle.MIUIX -> MiuixTheme.textStyles.main
}

/** Resolves the content color from the active native component tree. */
@Composable
@ReadOnlyComposable
internal fun currentAppContentColor(): Color = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> MaterialLocalContentColor.current
    AppUiStyle.MIUIX -> MiuixLocalContentColor.current
}
