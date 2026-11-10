// File: feature/audio/lyrics/LyricsWordAlignmentPolicy.kt
package com.android.purebilibili.feature.audio.lyrics

import kotlin.math.abs

/**
 * 词级对齐：把第三方逐字歌词（YRC / QRC / 增强 LRC 的词级时间轴）
 * 映射到 B 站字幕（CCU）的可信时间轴上。
 *
 * 两边都是同一首歌的逐字轴，理论上完全重合；实测会存在整体漂移与
 * 轻微线性伸缩（不同来源对前奏/间奏的计时口径不同）。做法：
 * 1. 用行起点做贪心最近邻配对；
 * 2. 配对数 ≥ 2 时按配对序列构建分段线性映射 t' = interp(t)，
 *    逐 span 重映射时间；
 * 3. 只有 1 个配对或完全无配对时，回退全局中位偏移 / 原轴；
 * 4. 音乐歌词行没有词级 span、而字幕侧有词级时间戳时，
 *    按字符比例把该行的字幕词时间分配给歌词文本，实现逐字高亮。
 */
internal object LyricsWordAlignmentPolicy {

    private const val LINE_MATCH_TOLERANCE_MS = 3_000L

    fun align(
        musicLyrics: LyricDocument,
        subtitleLyrics: LyricDocument
    ): LyricDocument {
        val pairs = matchLinePairs(musicLyrics.lines, subtitleLyrics.lines)
        val mapping = buildTimeMapper(pairs, musicLyrics.lines, subtitleLyrics.lines)
        return musicLyrics.copy(
            // 原轴匹配与显示校正分开；继承参考字幕偏移，同时保留歌词自己的手动微调。
            offsetMs = musicLyrics.offsetMs + subtitleLyrics.offsetMs,
            lines = musicLyrics.lines.map { line ->
                val mappedLine = LyricLine(
                    startTimeMs = mapping(line.startTimeMs),
                    endTimeMs = maxOf(mapping(line.endTimeMs), mapping(line.startTimeMs) + 1L),
                    text = line.text,
                    translations = line.translations,
                    romanization = line.romanization,
                    spans = if (line.spans.isNotEmpty()) {
                        line.spans.map { span ->
                            span.copy(
                                startTimeMs = mapping(span.startTimeMs),
                                endTimeMs = maxOf(mapping(span.endTimeMs), mapping(span.startTimeMs))
                            )
                        }
                    } else {
                        // 音乐行无词级时间：借用配对字幕行的词时间，逐字分配
                        distributeAcrossSubtitleWords(line, subtitleLyrics.lines, mapping)
                    }
                )
                mappedLine
            }
        )
    }

    private fun matchLinePairs(
        musicLines: List<LyricLine>,
        subtitleLines: List<LyricLine>
    ): List<Pair<Long, Long>> {
        val pairs = mutableListOf<Pair<Long, Long>>()
        var subtitleCursor = 0
        musicLines.forEach { musicLine ->
            var bestIndex = -1
            var bestDelta = LINE_MATCH_TOLERANCE_MS
            for (index in subtitleCursor until subtitleLines.size) {
                val delta = abs(subtitleLines[index].startTimeMs - musicLine.startTimeMs)
                if (delta <= bestDelta && lyricLinesMatch(musicLine.text, subtitleLines[index].text)) {
                    bestDelta = delta
                    bestIndex = index
                }
                if (subtitleLines[index].startTimeMs > musicLine.startTimeMs + LINE_MATCH_TOLERANCE_MS) break
            }
            if (bestIndex >= 0) {
                pairs += musicLine.startTimeMs to subtitleLines[bestIndex].startTimeMs
                subtitleCursor = bestIndex + 1
            }
        }
        return pairs
    }

    /**
     * 构建音乐时间轴 → 字幕时间轴的分段线性映射。
     * 配对不足 2 个时退化为全局中位偏移，再不足则恒等映射。
     */
    private fun buildTimeMapper(
        pairs: List<Pair<Long, Long>>,
        musicLines: List<LyricLine>,
        subtitleLines: List<LyricLine>
    ): (Long) -> Long {
        if (pairs.size >= 2) {
            val sorted = pairs.sortedBy { it.first }
            return { time ->
                when {
                    time <= sorted.first().first -> {
                        val (musicStart, subtitleStart) = sorted.first()
                        time - musicStart + subtitleStart
                    }
                    time >= sorted.last().first -> {
                        val (musicStart, subtitleStart) = sorted.last()
                        time - musicStart + subtitleStart
                    }
                    else -> {
                        var upperIndex = sorted.indexOfFirst { it.first >= time }
                        if (upperIndex <= 0) upperIndex = 1
                        val lower = sorted[upperIndex - 1]
                        val upper = sorted[upperIndex]
                        val span = (upper.first - lower.first).coerceAtLeast(1L)
                        val progress = (time - lower.first).toDouble() / span.toDouble()
                        (lower.second + (upper.second - lower.second) * progress).toLong()
                    }
                }
            }
        }
        if (pairs.size == 1) {
            val (musicStart, subtitleStart) = pairs.first()
            return { time -> time - musicStart + subtitleStart }
        }
        // 无配对：全局中位偏移
        val musicStarts = musicLines.map { it.startTimeMs }.sorted()
        val subtitleStarts = subtitleLines.map { it.startTimeMs }.sorted()
        if (musicStarts.isEmpty() || subtitleStarts.isEmpty() ||
            musicStarts.size != subtitleStarts.size
        ) {
            return { time -> time }
        }
        val offsets = musicStarts.zip(subtitleStarts) { music, subtitle -> subtitle - music }.sorted()
        val medianOffset = offsets[offsets.size / 2]
        return { time -> time + medianOffset }
    }

    /** 音乐行无词级时间时，把字幕配对行的词时间按字符占比分配给歌词文本。 */
    private fun distributeAcrossSubtitleWords(
        musicLine: LyricLine,
        subtitleLines: List<LyricLine>,
        mapping: (Long) -> Long
    ): List<LyricSpan> {
        val text = musicLine.text
        if (text.isBlank()) return emptyList()
        // 找与音乐行（已映射后）重叠最长的字幕行
        val mappedStart = mapping(musicLine.startTimeMs)
        val mappedEnd = maxOf(mapping(musicLine.endTimeMs), mappedStart + 1L)
        val target = subtitleLines
            .filter { line ->
                line.spans.isNotEmpty() && minOf(mappedEnd, line.endTimeMs) > maxOf(mappedStart, line.startTimeMs)
            }
            .maxByOrNull { line -> minOf(mappedEnd, line.endTimeMs) - maxOf(mappedStart, line.startTimeMs) }
            ?: return emptyList()
        val wordSpans = target.spans.mapNotNull { span ->
            val start = maxOf(span.startTimeMs, mappedStart)
            val end = minOf(span.endTimeMs, mappedEnd)
            span.copy(startTimeMs = start, endTimeMs = end).takeIf { end > start && span.text.isNotBlank() }
        }.sortedBy(LyricSpan::startTimeMs)
        if (wordSpans.isEmpty()) return emptyList()
        val totalChars = wordSpans.sumOf { it.text.length }.coerceAtLeast(1)
        fun timeAtCharacter(position: Double): Long {
            var consumed = 0
            wordSpans.forEach { span ->
                val next = consumed + span.text.length
                if (position <= next) {
                    val fraction = ((position - consumed) / span.text.length).coerceIn(0.0, 1.0)
                    return (span.startTimeMs + (span.endTimeMs - span.startTimeMs) * fraction).toLong()
                }
                consumed = next
            }
            return wordSpans.last().endTimeMs
        }
        // 短文本也覆盖整段词时间，不为每个来源词强塞一个字符而越界或提前唱完。
        return text.mapIndexed { index, character ->
            val start = timeAtCharacter(index.toDouble() * totalChars / text.length)
            val end = timeAtCharacter((index + 1).toDouble() * totalChars / text.length)
            LyricSpan(character.toString(), start, maxOf(start, end))
        }
    }
}
