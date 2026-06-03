package com.android.purebilibili.feature.video.screen

internal enum class VideoDetailTopBarAction {
    BACK,
    HOME
}

internal fun resolveVideoDetailTopBarAction(isHomeButton: Boolean): VideoDetailTopBarAction {
    return if (isHomeButton) {
        VideoDetailTopBarAction.HOME
    } else {
        VideoDetailTopBarAction.BACK
    }
}
