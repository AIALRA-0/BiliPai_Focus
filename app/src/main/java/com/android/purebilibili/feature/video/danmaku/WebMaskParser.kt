package com.android.purebilibili.feature.video.danmaku

import android.graphics.Path
import androidx.core.graphics.PathParser
import com.android.purebilibili.danmaku.engine.DanmakuMaskFrame
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.GZIPInputStream

internal data class WebMaskChunkIndex(
    val startTimeMs: Long,
    val offset: Int,
    val endOffset: Int
)

internal object WebMaskParser {
    private const val HEADER_SIZE = 16
    private const val INDEX_ENTRY_SIZE = 16
    private const val MAX_CHUNK_COUNT = 20_000
    private const val MAX_INFLATED_CHUNK_BYTES = 16 * 1024 * 1024
    private const val DEFAULT_CHUNK_DURATION_MS = 10_000L
    private const val SVG_PREFIX = "data:image/svg+xml;base64,"

    fun parseIndex(data: ByteArray): List<WebMaskChunkIndex> {
        if (data.size < HEADER_SIZE || !data.copyOfRange(0, 4).contentEquals("MASK".encodeToByteArray())) {
            return emptyList()
        }
        val header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        header.position(4)
        val version = header.int
        header.int // upstream vU, currently undocumented
        val count = header.int
        if (version != 1 || count !in 1..MAX_CHUNK_COUNT) return emptyList()
        val indexEnd = HEADER_SIZE.toLong() + count.toLong() * INDEX_ENTRY_SIZE
        if (indexEnd > data.size) return emptyList()

        val starts = LongArray(count)
        val offsets = IntArray(count)
        repeat(count) { index ->
            val start = header.long
            val rawOffset = header.long
            if (rawOffset !in indexEnd..data.size.toLong()) return emptyList()
            starts[index] = start.coerceAtLeast(0L)
            offsets[index] = rawOffset.toInt()
        }
        if (!offsets.asList().zipWithNext().all { (left, right) -> left < right }) return emptyList()

        return List(count) { index ->
            WebMaskChunkIndex(
                startTimeMs = starts[index],
                offset = offsets[index],
                endOffset = offsets.getOrNull(index + 1) ?: data.size
            )
        }
    }

    fun parseWindow(
        data: ByteArray,
        fps: Int,
        windowStartMs: Long,
        windowEndMs: Long
    ): List<DanmakuMaskFrame> {
        val index = parseIndex(data)
        if (index.isEmpty()) return emptyList()
        val safeFps = fps.coerceIn(1, 60)
        val safeStart = windowStartMs.coerceAtLeast(0L)
        val safeEnd = windowEndMs.coerceAtLeast(safeStart + 1L)
        val frames = ArrayList<DanmakuMaskFrame>()

        index.forEachIndexed chunkLoop@ { chunkIndex, chunk ->
            val chunkEnd = index.getOrNull(chunkIndex + 1)?.startTimeMs
                ?.takeIf { it > chunk.startTimeMs }
                ?: (chunk.startTimeMs + DEFAULT_CHUNK_DURATION_MS)
            if (chunkEnd < safeStart || chunk.startTimeMs > safeEnd) return@chunkLoop

            val inflated = inflateBounded(data, chunk.offset, chunk.endOffset) ?: return@chunkLoop
            if (inflated.size <= 16) return@chunkLoop
            val encodedSvgs = inflated.copyOfRange(16, inflated.size)
                .toString(Charsets.ISO_8859_1)
                .split(SVG_PREFIX)
                .drop(1)
                .filter(String::isNotBlank)
            if (encodedSvgs.isEmpty()) return@chunkLoop

            val inferredFrameDuration = ((chunkEnd - chunk.startTimeMs) / encodedSvgs.size)
                .takeIf { it > 0L }
                ?: (1_000L / safeFps).coerceAtLeast(1L)
            encodedSvgs.forEachIndexed frameLoop@ { frameIndex, encodedSvg ->
                val svg = decodeSvg(encodedSvg) ?: return@frameLoop
                val parsed = parseSvgPath(svg) ?: return@frameLoop
                val frameStart = chunk.startTimeMs + frameIndex * inferredFrameDuration
                val frameEnd = minOf(chunkEnd, frameStart + inferredFrameDuration)
                if (frameEnd >= safeStart && frameStart <= safeEnd) {
                    frames += DanmakuMaskFrame(
                        startTimeMs = frameStart,
                        endTimeMs = frameEnd,
                        path = parsed.path,
                        sourceWidth = parsed.width,
                        sourceHeight = parsed.height
                    )
                }
            }
        }
        return frames
    }

    private data class ParsedSvgPath(val path: Path, val width: Int, val height: Int)

    private fun parseSvgPath(svg: String): ParsedSvgPath? {
        val viewBox = VIEW_BOX_REGEX.find(svg)?.groupValues?.getOrNull(2)
            ?.trim()
            ?.split(Regex("[ ,]+"))
            ?.mapNotNull(String::toFloatOrNull)
        val width = viewBox?.getOrNull(2)?.toInt()?.coerceAtLeast(1) ?: 1920
        val height = viewBox?.getOrNull(3)?.toInt()?.coerceAtLeast(1) ?: 1080
        val combined = Path()
        var parsedAny = false
        PATH_DATA_REGEX.findAll(svg).forEach { match ->
            PathParser.createPathFromPathData(match.groupValues[2])?.let { path ->
                combined.addPath(path)
                parsedAny = true
            }
        }
        return if (parsedAny) ParsedSvgPath(combined, width, height) else null
    }

    private fun decodeSvg(encoded: String): String? = try {
        val base64Payload = encoded.takeWhile { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
        Base64.getDecoder().decode(base64Payload).toString(Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun inflateBounded(data: ByteArray, offset: Int, endOffset: Int): ByteArray? = try {
        GZIPInputStream(ByteArrayInputStream(data, offset, endOffset - offset)).use { gzip ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = gzip.read(buffer)
                if (read < 0) break
                if (output.size() + read > MAX_INFLATED_CHUNK_BYTES) return null
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    } catch (_: Exception) {
        null
    }

    private val VIEW_BOX_REGEX = Regex("""viewBox\s*=\s*([\"'])(.*?)\1""", RegexOption.IGNORE_CASE)
    private val PATH_DATA_REGEX = Regex("""<path\b[^>]*\bd\s*=\s*([\"'])(.*?)\1""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
}
