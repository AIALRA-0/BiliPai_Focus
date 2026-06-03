package com.android.purebilibili.feature.video.ui.section

internal fun resolveVideoPlayerOverlayHomeClick(
    onBack: () -> Unit,
    onHomeClick: (() -> Unit)?
): () -> Unit {
    return onHomeClick ?: onBack
}
