package com.android.purebilibili.feature.audio.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioNowPlayingSession {
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    fun markListening() {
        _active.value = true
    }

    fun dismiss() {
        _active.value = false
    }
}
