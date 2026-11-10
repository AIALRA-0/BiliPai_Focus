package com.android.purebilibili.feature.audio.player

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioNowPlayingSession {
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    /** 听视频小横条是否正悬浮在内容上方（由 AppNavigation 按可见性发布） */
    private val _barOverlayVisible = MutableStateFlow(false)
    val barOverlayVisible = _barOverlayVisible.asStateFlow()

    /** 沉浸式音乐页的封面主色，供悬浮小横条取色；离开音乐页时清空回退主题色 */
    private val _immersiveBackdropColor = MutableStateFlow<Color?>(null)
    val immersiveBackdropColor = _immersiveBackdropColor.asStateFlow()

    fun publishBarOverlayVisible(visible: Boolean) {
        _barOverlayVisible.value = visible
    }

    fun publishImmersiveBackdropColor(color: Color?) {
        _immersiveBackdropColor.value = color
    }

    fun markListening() {
        _active.value = true
    }

    fun dismiss() {
        _active.value = false
    }
}
