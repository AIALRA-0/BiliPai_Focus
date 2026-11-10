package com.android.bilipai.tv.ui

/** Focus is restored by identity; if removed, choose the nearest surviving visible item. */
fun resolveTvFocusIndex(ids: List<String>, focusedId: String?, previousIndex: Int): Int? {
    if (ids.isEmpty()) return null
    val exact = focusedId?.let { ids.indexOf(it) } ?: -1
    return if (exact >= 0) exact else previousIndex.coerceIn(0, ids.lastIndex)
}

fun tvSeekTarget(currentMs: Long, deltaMs: Long, durationMs: Long): Long? {
    if (durationMs <= 0) return null
    return (currentMs.coerceAtLeast(0) + deltaMs).coerceIn(0, durationMs)
}

enum class TvPlayerBackAction { CloseDialog, CancelSeek, HideControls, LeavePlayer }
fun resolveTvPlayerBack(dialogOpen: Boolean, seeking: Boolean, controlsVisible: Boolean) = when {
    dialogOpen -> TvPlayerBackAction.CloseDialog
    seeking -> TvPlayerBackAction.CancelSeek
    controlsVisible -> TvPlayerBackAction.HideControls
    else -> TvPlayerBackAction.LeavePlayer
}
