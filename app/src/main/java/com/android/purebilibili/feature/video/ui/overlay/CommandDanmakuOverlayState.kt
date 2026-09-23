package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.android.purebilibili.feature.video.danmaku.VoteOption

/** Owned by the video session, not by the viewport or currently visible command list. */
internal class CommandDanmakuOverlayState {
    private val dismissed = mutableStateMapOf<String, Boolean>()
    private val selections = mutableStateMapOf<String, VoteOption>()

    fun isDismissed(id: String): Boolean = dismissed[id] == true
    fun selection(id: String): VoteOption? = selections[id]
    fun dismiss(id: String) {
        dismissed[id] = true
    }
    fun select(id: String, option: VoteOption): Boolean {
        if (isDismissed(id) || selections.containsKey(id)) return false
        selections[id] = option
        return true
    }
}

@Composable
internal fun rememberCommandDanmakuOverlayState(contentKey: Any?): CommandDanmakuOverlayState =
    remember(contentKey) { CommandDanmakuOverlayState() }
