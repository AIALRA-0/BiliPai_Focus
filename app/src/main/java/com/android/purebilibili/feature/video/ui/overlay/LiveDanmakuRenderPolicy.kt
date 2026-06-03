package com.android.purebilibili.feature.video.ui.overlay

internal inline fun executeLiveDanmakuDataRefresh(
    pause: () -> Unit,
    setData: () -> Unit,
    start: () -> Unit,
    invalidateView: () -> Unit
) {
    pause()
    setData()
    start()
    invalidateView()
}
