package com.android.purebilibili.feature.video.viewmodel

internal fun shouldRebindVideoDomain(
    current: VideoSubjectSnapshot?,
    next: VideoSubjectSnapshot
): Boolean = current?.generation != next.generation
