package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


@Stable
internal class InlinePortraitPlayerCollapseState(
    initialOffsetPx: Float = 0f,
    initialRestoreRequested: Boolean = false
) {
    var offsetPx by mutableFloatStateOf(initialOffsetPx)
        private set

    var restoreRequested by mutableStateOf(initialRestoreRequested)
        private set

    fun updateOffset(value: Float) {
        offsetPx = value
    }

    fun reset() {
        offsetPx = 0f
        restoreRequested = false
    }

    fun beginScroll() {
        restoreRequested = false
    }

    fun restore() {
        offsetPx = 0f
        restoreRequested = true
    }

    companion object {
        val Saver = listSaver<InlinePortraitPlayerCollapseState, Any>(
            save = { listOf(it.offsetPx, it.restoreRequested) },
            restore = {
                InlinePortraitPlayerCollapseState(
                    initialOffsetPx = it[0] as Float,
                    initialRestoreRequested = it[1] as Boolean
                )
            }
        )
    }
}

@Composable
internal fun rememberInlinePortraitPlayerCollapseState(videoBvid: String) =
    rememberSaveable(videoBvid, saver = InlinePortraitPlayerCollapseState.Saver) {
        InlinePortraitPlayerCollapseState()
    }
