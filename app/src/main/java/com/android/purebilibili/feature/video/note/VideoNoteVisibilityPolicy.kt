package com.android.purebilibili.feature.video.note

internal fun shouldLoadVideoNote(
    isVideoNoteEnabled: Boolean,
    aid: Long
): Boolean {
    return isVideoNoteEnabled && aid > 0L
}

internal fun shouldShowVideoNoteCard(isVideoNoteEnabled: Boolean): Boolean {
    return isVideoNoteEnabled
}

internal fun shouldShowVideoNoteBody(
    defaultCollapsed: Boolean,
    userExpanded: Boolean
): Boolean {
    return !defaultCollapsed || userExpanded
}
