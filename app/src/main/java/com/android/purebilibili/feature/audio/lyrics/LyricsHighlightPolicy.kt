package com.android.purebilibili.feature.audio.lyrics

internal const val LYRIC_INACTIVE_ALPHA = 0.38f
internal const val LYRIC_ACTIVE_ALPHA = 1f

/** 0..1 progress of a word/phrase span at [positionMs]. */
internal fun resolveSpanHighlightProgress(
    span: LyricSpan,
    positionMs: Long,
): Float {
    val duration = span.endTimeMs - span.startTimeMs
    if (duration <= 0L) {
        return if (positionMs >= span.startTimeMs) 1f else 0f
    }
    return ((positionMs - span.startTimeMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

/** Line-level sweep when the source has no word timings. */
internal fun resolveLineSweepProgress(
    line: LyricLine,
    positionMs: Long,
): Float {
    val duration = line.endTimeMs - line.startTimeMs
    if (duration <= 0L) {
        return if (positionMs >= line.startTimeMs) 1f else 0f
    }
    return ((positionMs - line.startTimeMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

/**
 * Interpolates per-character alpha for karaoke-style fill.
 * [progress] is 0..1 across the whole run of [charCount] characters.
 */
internal fun resolveCharHighlightAlpha(
    charIndex: Int,
    charCount: Int,
    progress: Float,
    inactiveAlpha: Float = LYRIC_INACTIVE_ALPHA,
    activeAlpha: Float = LYRIC_ACTIVE_ALPHA,
): Float {
    if (charCount <= 0) return inactiveAlpha
    val charProgress = (progress.coerceIn(0f, 1f) * charCount - charIndex).coerceIn(0f, 1f)
    return inactiveAlpha + (activeAlpha - inactiveAlpha) * charProgress
}
