package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** 全窗口模态内容使用自己的菜单宿主，不复用父窗口的图层、锚点和焦点。 */
interface AppPopupWindowHostRenderer {
    @Composable
    fun Render(content: @Composable () -> Unit)
}

val LocalAppPopupWindowMaterialEnabled = staticCompositionLocalOf { false }

val LocalAppPopupWindowHostRenderer = staticCompositionLocalOf<AppPopupWindowHostRenderer?> { null }

@Composable
fun AppPopupWindowHost(
    enableWindowMaterial: Boolean = true,
    content: @Composable () -> Unit,
) {
    val renderer = LocalAppPopupWindowHostRenderer.current
    if (LocalAppThemeConfig.current.liquidGlassEnabled && renderer != null) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppPopupWindowMaterialEnabled provides enableWindowMaterial) {
            renderer.Render(content)
        }
    } else content()
}
