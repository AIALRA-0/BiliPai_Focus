package com.android.purebilibili.feature.video.viewmodel

import android.net.Uri

data class DanmakuComposerDraft(
    val text: String = "",
    val attentionCommand: Boolean = false
)

data class CommentComposerDraft(
    val text: String = "",
    val imageUris: List<Uri> = emptyList(),
    val syncToDynamic: Boolean = false
)

data class VideoComposerDraftState(
    val videoId: String = "",
    val danmaku: DanmakuComposerDraft = DanmakuComposerDraft(),
    val comments: Map<Long, CommentComposerDraft> = emptyMap()
)

internal fun commentComposerDraftKey(replyRpid: Long?): Long = replyRpid ?: 0L
