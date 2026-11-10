package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.blur.Backdrop

/** The app supplies its glass material; the design system owns placement and scope. */
interface AppTopBarIconDecorator {
    @Composable
    fun Render(modifier: Modifier, enabled: Boolean, content: @Composable (Modifier) -> Unit)
}

val LocalAppTopBarIconDecorator = staticCompositionLocalOf<AppTopBarIconDecorator?> { null }
val LocalAppTopBarIconDecorationEnabled = staticCompositionLocalOf { false }
/** 图标下方实际顶栏容器的遮色，独立于底层内容录制。 */
val LocalAppTopBarSurfaceColor = staticCompositionLocalOf { androidx.compose.ui.graphics.Color.Transparent }

val LocalAppTopBarIconBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** True only when the surrounding decorator actually drew a glass material. */
val LocalAppIconButtonGlassSurfaceProvided = staticCompositionLocalOf { false }
