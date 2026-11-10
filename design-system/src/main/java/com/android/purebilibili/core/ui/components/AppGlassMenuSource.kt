package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** 记录实际触发控件，保留布局与指针命中，只在菜单形变期间接管其绘制。 */
@Stable
class AppGlassMenuSource internal constructor(
    internal val enabled: Boolean,
    internal val layer: GraphicsLayer,
) {
    internal var bounds by mutableStateOf<Rect?>(null)
    internal var visible by mutableStateOf(true)
    internal var press: AppSingleChoicePopupPress? = null
    val isAvailable: Boolean get() = enabled && bounds?.isEmpty == false && !layer.isReleased
}

@Composable
fun rememberAppGlassMenuSource(): AppGlassMenuSource {
    val enabled = appGlassMenuAvailable()
    val layer = rememberGraphicsLayer()
    val source = remember(enabled, layer) { AppGlassMenuSource(enabled, layer) }
    DisposableEffect(source) { onDispose { source.press?.cancel(); source.press = null; source.bounds = null; source.visible = true } }
    return source
}

fun Modifier.appGlassMenuSource(
    source: AppGlassMenuSource,
    onOpen: (() -> Unit)? = null,
    enabled: Boolean = true,
    // 外层已捕获完整玻璃时，子控件只保留按住打开手势，不覆盖外层边界与图层。
    captureSurface: Boolean = true,
): Modifier {
    if (!source.enabled) return this
    return this.composed {
        val pressModifier = rememberChoicePopupPressModifier(
            enabled = enabled && onOpen != null,
            consumeInInitial = true,
            toWindow = { (source.bounds?.topLeft ?: androidx.compose.ui.geometry.Offset.Zero) + it },
            onOpen = { press -> source.press?.cancel(); source.press = press; onOpen?.invoke() },
        )
        if (!captureSurface) return@composed pressModifier
        Modifier.then(pressModifier)
            .onGloballyPositioned { source.bounds = it.boundsInWindow() }
            .drawWithContent {
                source.layer.record { this@drawWithContent.drawContent() }
                if (source.visible) drawLayer(source.layer)
            }
    }
}

val LocalAppGlassMenuSource = androidx.compose.runtime.compositionLocalOf<AppGlassMenuSource?> { null }

/** 只包裹触发控件与菜单入口，不包裹页面内容或长按卡片。 */
@Composable
fun AppGlassMenuAnchor(
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val source = rememberAppGlassMenuSource()
    androidx.compose.foundation.layout.Box(modifier.appGlassMenuSource(source, onOpen, enabled)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppGlassMenuSource provides source) { content() }
    }
}
