package com.android.purebilibili.feature.video.subtitle

import com.android.purebilibili.core.player.PlaybackPositionTimeline

/** 字幕结束毫秒仍属于当前句，因此消失边界取下一毫秒；正偏移表示延后显示。 */
internal fun subtitlePositionTimeline(
    primary: List<SubtitleCue>,
    secondary: List<SubtitleCue> = emptyList(),
    offsetMs: Long = 0L,
): PlaybackPositionTimeline = PlaybackPositionTimeline(
    sequenceOf(primary, secondary).flatten().flatMap { cue ->
        sequenceOf(cue.startMs + offsetMs, cue.endMs + offsetMs + 1L)
    }.asIterable(),
)
