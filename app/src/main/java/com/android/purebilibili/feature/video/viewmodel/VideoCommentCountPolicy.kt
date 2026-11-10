package com.android.purebilibili.feature.video.viewmodel

/** Server counts win, including zero. Seed the first frame from this subject's detail metadata. */
internal fun CommentUiState.resolveReplyCount(
    subject: CommentSubjectKey,
    detailCount: Int? = null,
): Int? {
    val sameSubject = replyCountSubject == subject
    return when {
        sameSubject && replyCountIsAuthoritative -> replyCount.coerceAtLeast(0)
        detailCount != null -> detailCount.coerceAtLeast(0)
        sameSubject -> replyCount.coerceAtLeast(0)
        else -> null
    }
}
