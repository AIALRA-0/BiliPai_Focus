package com.android.purebilibili.feature.video.subtitle

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

enum class SubtitleDocumentFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"), WEBVTT("vtt", "text/vtt"), SRT("srt", "application/x-subrip")
}

data class SubtitleDocumentIdentity(val bvid: String, val cid: Long)

/** Positive offsets delay subtitles; this is unrelated to dragging their on-screen position. */
fun subtitlePositionForOffset(positionMs: Long, offsetMs: Long): Long = positionMs - offsetMs

fun subtitleDocumentResultBelongsTo(request: SubtitleDocumentIdentity, current: SubtitleDocumentIdentity): Boolean =
    request == current && request.bvid.isNotBlank() && request.cid > 0L

fun decodeSubtitleDocument(bytes: ByteArray): String {
    val charset = when {
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }
    return bytes.toString(charset).removePrefix("\uFEFF")
}

fun parseSubtitleDocument(fileName: String, source: String): List<SubtitleCue> {
    val raw = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val cues = if (extension in setOf("json", "bcc") || raw.trimStart().startsWith("{")) {
        parseBiliSubtitleBody(raw)
    } else {
        val mimeType = when {
            extension in setOf("ass", "ssa") || raw.contains("[Events]", ignoreCase = true) -> MimeTypes.TEXT_SSA
            extension == "vtt" || raw.trimStart().startsWith("WEBVTT") -> MimeTypes.TEXT_VTT
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        parseStandardSubtitle(raw, mimeType)
    }.filter { it.endMs > it.startMs && it.content.isNotBlank() }.sortedBy { it.startMs }
    require(cues.isNotEmpty()) { "未找到有效字幕；支持 JSON/BCC、WEBVTT、SRT、ASS" }
    return cues
}

/** 标准格式由媒体解析器处理；转换为本项目字幕工具共用的毫秒文本时间轴。 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun parseStandardSubtitle(source: String, mimeType: String): List<SubtitleCue> {
    val format = Format.Builder().setSampleMimeType(mimeType).build()
    val parser = DefaultSubtitleParserFactory().create(format)
    val bytes = source.toByteArray(Charsets.UTF_8)
    return buildList {
        parser.parse(bytes, SubtitleParser.OutputOptions.allCues()) { timed ->
            if (timed.startTimeUs != C.TIME_UNSET && timed.endTimeUs != C.TIME_UNSET) {
                val text = timed.cues.mapNotNull { it.text?.toString()?.replace('\u00A0', ' ')?.trim()?.takeIf(String::isNotBlank) }
                    .joinToString("\n")
                if (text.isNotBlank()) {
                    add(SubtitleCue(timed.startTimeUs / 1000, timed.endTimeUs / 1000, text))
                }
            }
        }
    }
}

fun encodeSubtitleDocument(cues: List<SubtitleCue>, format: SubtitleDocumentFormat): String = when (format) {
    SubtitleDocumentFormat.JSON -> buildJsonObject {
        put("body", buildJsonArray {
            cues.forEach { cue -> add(buildJsonObject {
                put("from", cue.startMs / 1000.0); put("to", cue.endMs / 1000.0); put("content", cue.content)
            }) }
        })
    }.toString()
    SubtitleDocumentFormat.WEBVTT, SubtitleDocumentFormat.SRT -> buildString {
        if (format == SubtitleDocumentFormat.WEBVTT) append("WEBVTT\n\n")
        cues.forEachIndexed { index, cue ->
            if (format == SubtitleDocumentFormat.SRT) append(index + 1).append('\n')
            append(formatSubtitleTimestamp(cue.startMs, format)).append(" --> ")
                .append(formatSubtitleTimestamp(cue.endMs, format)).append('\n')
                .append(if (format == SubtitleDocumentFormat.WEBVTT) cue.content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") else cue.content)
                .append("\n\n")
        }
    }
}

private fun formatSubtitleTimestamp(ms: Long, format: SubtitleDocumentFormat): String {
    val time = ms.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d%s%03d", time / 3_600_000, time / 60_000 % 60,
        time / 1000 % 60, if (format == SubtitleDocumentFormat.SRT) "," else ".", time % 1000)
}
