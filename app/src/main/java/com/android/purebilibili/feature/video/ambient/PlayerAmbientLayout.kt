package com.android.purebilibili.feature.video.ambient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/** Owns the glow outside player clipping/shared bounds, with a real gutter before body content.
 * playerModifier retains the original video sizing; modifier places the entire host.
 */
@Composable
internal fun PlayerAmbientLayout(
    playerModifier: Modifier,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // Reuse a surrounding host when the tablet player delegates to the phone player slot.
    if (LocalAmbientPresentation.current != null) {
        Box(modifier = modifier.then(playerModifier), content = content)
        return
    }
    val presentation = remember { AmbientPresentation() }
    val controller = remember(presentation) { AmbientFrameController(presentation) }
    CompositionLocalProvider(
        LocalAmbientPresentation provides presentation,
        LocalAmbientController provides controller,
    ) {
        //  [内联环境光] 播放器下缘不做黑色→页面背景的过渡底座：浅色主题下读起来
        //  像一条阴影带，与"取消底部沉浸光"的设计一致，视频下缘直接过渡到页面。
        Box(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!fullscreen) PlayerAmbientGlow(presentation, fullscreen = false, modifier = Modifier.matchParentSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = playerModifier.onGloballyPositioned {
                        val origin = it.positionInWindow()
                        presentation.inlineBoundsInWindow = Rect(
                            origin.x, origin.y,
                            origin.x + it.size.width, origin.y + it.size.height,
                        )
                    },
                    content = content,
                )
                //  [内联环境光] 底部光晕与预留槽位已随"取消底部沉浸光"一并移除，
                //  播放器下缘直接衔接简介区，不再保留 48dp 空白。
            }
        }
    }
}
