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

            // AI 逐字字幕：把词级时间戳转成逐字 span，供逐字高亮渲染
            val spans = cue.words
                .filter { it.text.isNotBlank() && it.endMs > it.startMs }
                .map { word ->
                    LyricSpan(
                        text = word.text,
                        startTimeMs = word.startMs,
                        endTimeMs = word.endMs
                    )
                }
                .takeIf { it.isNotEmpty() }
                ?: distributeTextEvenly(cue.content.trim(), cue.startMs, cue.endMs)

            LyricLine(
                startTimeMs = cue.startMs,
                endTimeMs = cue.endMs,
                text = cue.content.trim(),
                translations = listOfNotNull(translation?.takeIf { it.isNotEmpty() }),
                spans = spans
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
     * 带质量评估的取舍：手动选择保持原轴；自动歌词同时检查内容与时间的一一对应。
     * 搜索歌词若整体漂移大或行覆盖低（常见于串歌、Live 版、时长不符），
     * 则回退到与播放时间轴天然一致的 B 站字幕，避免高亮错位。
     */
    fun resolveEffectiveLyricsWithAlignment(
        musicLyrics: LyricDocument?,
        subtitleLyrics: LyricDocument?
    ): LyricDocument? {
        val music = musicLyrics?.takeIf { it.lines.isNotEmpty() } ?: return resolveEffectiveLyrics(null, subtitleLyrics)
        if (music.manuallySelected || music.source == LyricSource.MANUAL) return music
        val subtitle = subtitleLyrics?.takeIf { it.lines.isNotEmpty() }
            ?: return resolveEffectiveLyrics(music, null)

        val alignment = scoreTimelineAlignment(
            reference = subtitle,
            candidate = music
        )
        if (alignment < LYRIC_ALIGNMENT_MINIMUM_SCORE) return subtitle
        // 对齐可信：把搜索歌词的行/词时间轴映射到字幕轴，消除整体漂移与伸缩
        return LyricsWordAlignmentPolicy.align(music, subtitle)
    }

    /**
     * 无词级时间戳的行（普通 CC 字幕）按字符数把行时长均匀切分成 span，
     * 与逐字歌词共用同一套高亮渲染。
     */
    private fun distributeTextEvenly(text: String, startMs: Long, endMs: Long): List<LyricSpan> {
        if (text.isBlank()) return emptyList()
        val durationMs = (endMs - startMs).coerceAtLeast(0L)
        if (durationMs <= 0L) return emptyList()
        val stepMs = durationMs.toDouble() / text.length
        var cursor = startMs.toDouble()
        return text.map { character ->
            val spanStart = cursor
            cursor += stepMs
            LyricSpan(
                text = character.toString(),
                startTimeMs = spanStart.toLong(),
                endTimeMs = cursor.toLong()
            )
        }
    }

    /** 搜索歌词时间轴相对字幕时间轴的对齐分数，1 = 完全贴合，0 = 完全错位。 */
    fun scoreTimelineAlignment(
        reference: LyricDocument,
        candidate: LyricDocument
    ): Double {
        if (reference.lines.isEmpty() || candidate.lines.isEmpty()) return 0.0
        val toleranceMs = 1_500L
        val offsets = mutableListOf<Long>()
        val usedCandidateIndices = mutableSetOf<Int>()
        reference.lines.forEach { line ->
            val nearest = candidate.lines.indices
                .filter { index ->
                    index !in usedCandidateIndices &&
                        kotlin.math.abs(candidate.lines[index].startTimeMs - line.startTimeMs) <= toleranceMs &&
                        lyricLinesMatch(line.text, candidate.lines[index].text)
                }
                .minByOrNull { kotlin.math.abs(candidate.lines[it].startTimeMs - line.startTimeMs) }
                ?: return@forEach
            offsets += candidate.lines[nearest].startTimeMs - line.startTimeMs
            usedCandidateIndices += nearest
        }
        if (offsets.isEmpty()) return 0.0
        val coverage = offsets.size.toDouble() / maxOf(reference.lines.size, candidate.lines.size).toDouble()
        if (coverage < 0.5) return 0.0
        // 覆盖率为主（行是否对得上），紧密度为辅（整体漂移是否小）
        val compactness = 1.0 - (
            offsets.map { kotlin.math.abs(it) }.average() / toleranceMs.toDouble()
            ).coerceIn(0.0, 1.0)
        return (coverage * 0.75 + compactness * 0.25).coerceIn(0.0, 1.0)
    }

    private const val LYRIC_ALIGNMENT_MINIMUM_SCORE = 0.45

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
