package com.android.purebilibili.feature.audio.lyrics

private val metadataPattern = Regex("^\\[([A-Za-z]+):(.*)]$")
private val leadingTimestampPattern = Regex("^(?:\\[-?\\d{1,3}:[-?\\d]{1,2}\\.\\d{1,6}])+")
private val timestampPattern = Regex("\\[(-?\\d{1,3}):(-?\\d{1,2})\\.(\\d{1,6})]")
private val spanTimestampPattern = Regex("<(-?\\d{1,3}):(-?\\d{1,2})\\.(\\d{1,6})>")

private data class ParsedRawLine(
    val startTimeMs: Long,
    val text: String,
    val spans: List<LyricSpan>,
    val explicitEndTimeMs: Long?
)

internal fun parseSplLyrics(
    primary: String,
    translation: String? = null,
    romanization: String? = null,
    source: LyricSource = LyricSource.BILIBILI
): LyricDocument {
    val metadata = linkedMapOf<String, String>()
    val primaryLines = mergeSimultaneousLines(parseRawLines(primary, metadata))
    val translationLines = translation
        ?.let { parseRawLines(it, linkedMapOf()) }
        .orEmpty()
    val romanizationLines = romanization
        ?.let { parseRawLines(it, linkedMapOf()) }
        .orEmpty()

    val primaryTimeline = primaryLines.map { raw ->
        LyricLine(raw.startTimeMs, raw.startTimeMs, raw.text)
    }
    val translationMap = mergeAuxiliaryLyrics(
        primaryTimeline,
        translationLines.map { raw -> LyricLine(raw.startTimeMs, raw.startTimeMs, raw.text) }
    )
    val romanizationMap = mergeAuxiliaryLyrics(
        primaryTimeline,
        romanizationLines.map { raw -> LyricLine(raw.startTimeMs, raw.startTimeMs, raw.text) }
    )

    val lines = normalizeLyricTimeline(primaryLines
        .map { raw ->
            val nextStart = primaryLines
                .asSequence()
                .map { it.startTimeMs }
                .filter { it > raw.startTimeMs }
                .minOrNull()
            val endTime = raw.explicitEndTimeMs
                ?: nextStart
                ?: (raw.startTimeMs + 10_000L)
            LyricLine(
                startTimeMs = raw.startTimeMs,
                endTimeMs = endTime.coerceAtLeast(raw.startTimeMs),
                text = raw.text,
                translations = translationMap[raw.startTimeMs].orEmpty(),
                romanization = romanizationMap[raw.startTimeMs]?.firstOrNull(),
                spans = raw.spans
                    .filter { span -> span.startTimeMs < endTime }
                    .map { span ->
                        span.copy(
                            endTimeMs = when {
                                span.endTimeMs <= span.startTimeMs -> endTime
                                else -> minOf(span.endTimeMs, endTime)
                            }.coerceAtLeast(span.startTimeMs)
                        )
                    }
            )
        })

    return LyricDocument(
        metadata = metadata,
        lines = lines,
        source = source
    )
}

private fun mergeSimultaneousLines(lines: List<ParsedRawLine>): List<ParsedRawLine> {
    return lines
        .groupBy(ParsedRawLine::startTimeMs)
        .toSortedMap()
        .map { (startTimeMs, simultaneous) ->
            val distinct = simultaneous.distinctBy(ParsedRawLine::text)
            ParsedRawLine(
                startTimeMs = startTimeMs,
                text = distinct.map(ParsedRawLine::text).joinToString("\n"),
                spans = if (distinct.size == 1) distinct.single().spans else emptyList(),
                explicitEndTimeMs = simultaneous.mapNotNull(ParsedRawLine::explicitEndTimeMs)
                    .maxOrNull()
            )
        }
}

private fun parseRawLines(
    content: String,
    metadata: MutableMap<String, String>
): List<ParsedRawLine> {
    val result = mutableListOf<ParsedRawLine>()
    content.lineSequence().forEach { input ->
        val line = input.trim()
        if (line.isEmpty()) return@forEach

        metadataPattern.matchEntire(line)?.let { match ->
            metadata[match.groupValues[1]] = match.groupValues[2].trim()
            return@forEach
        }

        val leading = leadingTimestampPattern.find(line) ?: return@forEach
        if (leading.range.first != 0) return@forEach
        val lyricContent = line.substring(leading.value.length)
        val timestamps = timestampPattern.findAll(leading.value)
            .mapNotNull { parseTimestampMs(it.groupValues) }
            .distinct()
            .toList()
        timestamps.forEach { timestamp ->
            val spanResult = parseSpans(lyricContent, timestamp)
            result += ParsedRawLine(
                startTimeMs = timestamp,
                text = spanResult.text,
                spans = spanResult.spans,
                explicitEndTimeMs = spanResult.explicitEndTimeMs
            )
        }
    }
    return result
}

private data class ParsedSpans(
    val text: String,
    val spans: List<LyricSpan>,
    val explicitEndTimeMs: Long?
)

private fun parseSpans(content: String, lineStartTimeMs: Long): ParsedSpans {
    val matches = spanTimestampPattern.findAll(content).toList()
    if (matches.isEmpty()) {
        return ParsedSpans(content, emptyList(), null)
    }

    val spans = mutableListOf<LyricSpan>()
    val text = StringBuilder()
    var cursor = 0
    var currentStart = lineStartTimeMs
    matches.forEach { match ->
        val segment = content.substring(cursor, match.range.first)
        val timestamp = parseTimestampMs(match.groupValues) ?: currentStart
        if (segment.isNotEmpty()) {
            spans += LyricSpan(
                text = segment,
                startTimeMs = currentStart,
                endTimeMs = timestamp.coerceAtLeast(currentStart)
            )
            text.append(segment)
        }
        currentStart = timestamp.coerceAtLeast(currentStart)
        cursor = match.range.last + 1
    }

    val tail = content.substring(cursor)
    if (tail.isNotEmpty()) {
        spans += LyricSpan(tail, currentStart, 0L)
        text.append(tail)
    }
    val explicitEnd = matches.lastOrNull()
        ?.takeIf { it.range.last == content.lastIndex }
        ?.let { parseTimestampMs(it.groupValues) }

    return ParsedSpans(text.toString(), spans, explicitEnd)
}

private fun parseTimestampMs(groups: List<String>): Long? {
    val minutes = groups.getOrNull(1)?.toLongOrNull() ?: return null
    val seconds = groups.getOrNull(2)?.toLongOrNull() ?: return null
    val fraction = groups.getOrNull(3).orEmpty()
    if (seconds !in 0L..59L || fraction.isEmpty()) return null
    val millis = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: return null
    return (minutes * 60_000L) + (seconds * 1_000L) + millis
}
