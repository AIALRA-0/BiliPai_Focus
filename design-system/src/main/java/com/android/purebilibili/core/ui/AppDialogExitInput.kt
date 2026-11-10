package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

/** 已关闭的业务只保留退出画面；观察器始终存在，也能拦住关闭前已按下的第二根手指。 */
@Composable
internal fun Modifier.blockAppDialogExitInput(closing: Boolean): Modifier {
    val latestClosing by rememberUpdatedState(closing)
    return this
        .then(if (closing) Modifier.clearAndSetSemantics { } else Modifier)
        .onPreviewKeyEvent { latestClosing }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (latestClosing) event.changes.forEach { it.consume() }
                }
            }
        }
}
