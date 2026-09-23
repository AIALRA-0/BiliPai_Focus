package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3Badge
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixBadge

@Composable
fun AppBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Badge(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
        AppUiStyle.MIUIX -> AppMiuixBadge(
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
    }
}
