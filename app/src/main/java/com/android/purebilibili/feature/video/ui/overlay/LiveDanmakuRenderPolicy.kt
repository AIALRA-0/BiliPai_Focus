package com.android.purebilibili.feature.video.ui.overlay

internal inline fun <T> appendLiveDanmakuBatch(
    batch: List<T>,
    append: (List<T>) -> Unit
) {
    if (batch.isNotEmpty()) append(batch)
}

internal fun shouldRenderLiveDanmakuAsBitmap(
    isSuperChat: Boolean,
    emoticonUrl: String?
): Boolean = isSuperChat || !emoticonUrl.isNullOrBlank()
