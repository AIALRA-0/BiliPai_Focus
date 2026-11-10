package com.android.purebilibili.data.repository

import android.util.Log
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.danmaku.parser.IndexedSpecialDanmakuSource
import com.android.purebilibili.danmaku.parser.SpecialDanmakuIndexReader
import com.android.purebilibili.danmaku.parser.SpecialDanmakuSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

/** Only the small byte/time index survives the session. No playback files are written to disk. */
internal fun indexSpecialDanmakuSources(
    urls: List<String> = emptyList(),
    localPaths: List<String> = emptyList()
): Flow<IndexedSpecialDanmakuSource> = channelFlow {
    val permits = Semaphore(4)
    val requests = urls.map { url -> suspend { RemoteSpecialDanmakuSource.open(url) } } +
        localPaths.map { path -> suspend { LocalSpecialDanmakuSource(File(path)) } }
    requests.forEach { open ->
        launch(Dispatchers.IO) {
            permits.withPermit {
                try {
                    val source = open()
                    val reader = SpecialDanmakuIndexReader(source.byteLength, source::readRange)
                    val entries = buildList {
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            add(reader.next() ?: break)
                        }
                    }
                    Log.d("DanmakuRepo", "Special range index: bytes=${source.byteLength}, entries=${entries.size}")
                    send(IndexedSpecialDanmakuSource(source, entries))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("DanmakuRepo", "Special danmaku index failed", e)
                }
            }
        }
    }
}

private class LocalSpecialDanmakuSource(private val file: File) : SpecialDanmakuSource {
    override val byteLength: Long = file.length()

    override suspend fun readRange(offset: Long, byteCount: Int): ByteArray = withContext(Dispatchers.IO) {
        checkRange(offset, byteCount, byteLength)
        currentCoroutineContext().ensureActive()
        val bytes = ByteArray(byteCount)
        RandomAccessFile(file, "r").use { input ->
            input.seek(offset)
            input.readFully(bytes)
        }
        bytes
    }
}

private class RemoteSpecialDanmakuSource(
    private val url: String,
    override val byteLength: Long,
    private val prefix: ByteArray
) : SpecialDanmakuSource {
    companion object {
        private val contentRangePattern = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)")

        suspend fun open(rawUrl: String): RemoteSpecialDanmakuSource = withContext(Dispatchers.IO) {
            val url = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl
            val response = NetworkModule.api.getDanmakuSpecialRange(url, "bytes=0-1023")
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                throw HttpException(response)
            }
            val body = response.body() ?: throw IOException("Empty special danmaku response")
            body.use {
                val range = if (response.code() == 206) decodeRange(response.headers()["Content-Range"]) else null
                if (range != null && range.first != 0L) throw IOException("Incorrect initial special byte range")
                val length = range?.third ?: body.contentLength()
                if (length <= 0L) throw IOException("Missing special danmaku byte length")
                val prefix = readExactly(body.byteStream(), minOf(1024L, length).toInt())
                RemoteSpecialDanmakuSource(url, length, prefix)
            }
        }

        private fun decodeRange(header: String?): Triple<Long, Long, Long> {
            val match = header?.let(contentRangePattern::matchEntire)
                ?: throw IOException("Missing special danmaku Content-Range")
            val values = match.groupValues.drop(1).map {
                it.toLongOrNull() ?: throw IOException("Invalid special danmaku Content-Range")
            }
            if (values[0] > values[1] || values[1] >= values[2]) throw IOException("Invalid special byte bounds")
            return Triple(values[0], values[1], values[2])
        }
    }

    override suspend fun readRange(offset: Long, byteCount: Int): ByteArray = withContext(Dispatchers.IO) {
        checkRange(offset, byteCount, byteLength)
        if (offset == 0L && byteCount == prefix.size) return@withContext prefix
        val response = NetworkModule.api.getDanmakuSpecialRange(url, "bytes=$offset-${offset + byteCount - 1}")
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw HttpException(response)
        }
        val body = response.body() ?: throw IOException("Empty special danmaku range")
        body.use {
            val input = body.byteStream()
            when (response.code()) {
                206 -> {
                    val range = decodeRange(response.headers()["Content-Range"])
                    if (range.first != offset || range.second - range.first + 1 < byteCount || range.third != byteLength) {
                        throw IOException("Incorrect special danmaku byte range")
                    }
                }
                200 -> {
                    // A server without Range support is read-and-discarded, never buffered/cached whole.
                    var remaining = offset
                    val discard = ByteArray(8192)
                    while (remaining > 0L) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(discard, 0, minOf(remaining, discard.size.toLong()).toInt())
                        if (count <= 0) throw EOFException("Truncated special danmaku source")
                        remaining -= count
                    }
                }
                else -> throw IOException("Unexpected special danmaku response ${response.code()}")
            }
            readExactly(input, byteCount)
        }
    }
}

private fun checkRange(offset: Long, byteCount: Int, byteLength: Long) {
    require(offset >= 0L && byteCount > 0 && offset <= byteLength && byteCount.toLong() <= byteLength - offset)
}

private suspend fun readExactly(input: InputStream, count: Int): ByteArray {
    val bytes = ByteArray(count)
    var offset = 0
    while (offset < count) {
        currentCoroutineContext().ensureActive()
        val read = input.read(bytes, offset, minOf(8192, count - offset))
        if (read <= 0) throw EOFException("Truncated special danmaku range")
        offset += read
    }
    return bytes
}
