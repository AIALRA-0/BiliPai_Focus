package com.android.purebilibili.feature.audio.lyrics

import com.android.purebilibili.feature.video.subtitle.SubtitleCue

/**
 * 将 B 站视频字幕（原生 CC 字幕或 AI 语音生成字幕）转换为音乐播放器通用的 [LyricDocument]。
 * 支持主副双语字幕时间轴对齐，并保持与传统音乐歌词完全一致的滚动、毛玻璃虚化与高亮渲染。
 */
internal object BiliSubtitleLyricsPolicy {

    /**
     * 将给定的字幕条目列表转换为 [LyricDocument]。
     * 若字幕列表为空或全部为空白内容，则返回 null。
     */
    fun convertSubtitlesToLyricDocument(
        primaryCues: List<SubtitleCue>,
        secondaryCues: List<SubtitleCue> = emptyList(),
        isAiGenerated: Boolean = false,
        languageLabel: String? = null
    ): LyricDocument? {
        val validPrimary = primaryCues.filter { cue ->
            cue.content.isNotBlank() && cue.endMs > cue.startMs
        }
        if (validPrimary.isEmpty()) return null

        val lines = validPrimary.map { cue ->
            // 匹配在主字幕发音区间内的副字幕（如双语翻译）
            val translation = secondaryCues.firstOrNull { sec ->
                sec.content.isNotBlank() &&
                    maxOf(cue.startMs, sec.startMs) < minOf(cue.endMs, sec.endMs)
            }?.content?.trim()

            LyricLine(
                startTimeMs = cue.startMs,
                endTimeMs = cue.endMs,
                text = cue.content.trim(),
                translations = listOfNotNull(translation?.takeIf { it.isNotEmpty() })
            )
        }

        val typeLabel = if (isAiGenerated) "AI 字幕" else "B站字幕"
        val displayLabel = languageLabel?.takeIf { it.isNotBlank() } ?: typeLabel

        return LyricDocument(
            lines = lines,
            source = LyricSource.BILIBILI,
            metadata = mapOf(
                "source_type" to if (isAiGenerated) "ai_subtitle" else "cc_subtitle",
                "label" to displayLabel
            )
        )
    }

    /**
     * 根据优先级决策当前应该生效的歌词/字幕文档：
     * 1. 优先使用外部音乐服务（网易云/QQ音乐/酷狗）或用户手动选择的高质量歌曲歌词；
     * 2. 若无音乐歌词或匹配未成功，则回退使用视频自带的原生/AI 字幕；
     * 3. 若均无则返回 null。
     */
    fun resolveEffectiveLyrics(
        musicLyrics: LyricDocument?,
        subtitleLyrics: LyricDocument?
    ): LyricDocument? {
        if (musicLyrics != null && musicLyrics.lines.isNotEmpty()) {
            return musicLyrics
        }
        if (subtitleLyrics != null && subtitleLyrics.lines.isNotEmpty()) {
            return subtitleLyrics
        }
        return null
    }

    /**
     * 获取当前歌词/字幕来源的可读标签（用于设置面板或调试展示）。
     */
    fun resolveSourceLabel(document: LyricDocument?): String {
        if (document == null) return ""
        return when (document.source) {
            LyricSource.BILIBILI -> document.metadata["label"] ?: "B站字幕"
            LyricSource.NETEASE -> "网易云音乐"
            LyricSource.QQ_MUSIC -> "QQ音乐"
            LyricSource.KUGOU -> "酷狗音乐"
            LyricSource.MANUAL -> "本地/手动"
        }
    }
}
