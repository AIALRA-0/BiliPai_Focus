package com.android.purebilibili.feature.live

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/** 鼠标次键执行同一内容的长按操作，不触发打开直播间。 */
internal fun Modifier.liveSecondaryClick(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) this else pointerInput(onClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    onClick()
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
