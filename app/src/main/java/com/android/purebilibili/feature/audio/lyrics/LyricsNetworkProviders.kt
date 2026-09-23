package com.android.purebilibili.feature.audio.lyrics

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private val lyricsJson = Json { ignoreUnknownKeys = true }

internal class NeteaseLyricsProvider(
    private val client: OkHttpClient
) : LyricsProvider {
    override val source = LyricSource.NETEASE

    override suspend fun search(query: LyricQuery): List<LyricCandidate> {
        val keyword = urlEncode("${query.title} ${query.artist}")
        return parseNeteaseCandidates(
            client.getBody("https://music.163.com/api/search/get?s=$keyword&type=1&limit=10&offset=0")
        )
    }

    override suspend fun fetch(candidate: LyricCandidate): RawLyrics {
        return parseNeteaseLyrics(
            client.getBody(
                "https://music.163.com/api/song/lyric?id=${urlEncode(candidate.remoteId)}&lv=-1&tv=-1&rv=-1&kv=-1&yv=-1"
            )
        )
    }
}

internal class QqMusicLyricsProvider(
    private val client: OkHttpClient
) : LyricsProvider {
    override val source = LyricSource.QQ_MUSIC

    override suspend fun search(query: LyricQuery): List<LyricCandidate> {
        val keyword = urlEncode("${query.title} ${query.artist}")
        return parseQqCandidates(
            client.getBody(
                "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?key=$keyword&format=json",
                qqHeaders
            )
        )
    }

    override suspend fun fetch(candidate: LyricCandidate): RawLyrics {
        return parseQqLyrics(
            client.getBody(
                "https://i.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${urlEncode(candidate.remoteId)}&g_tk=5381&format=json&inCharset=utf8&outCharset=utf-8&nobase64=1",
                qqHeaders
            )
        )
    }

    private companion object {
        val qqHeaders = mapOf(
            "Referer" to "https://y.qq.com/",
            "User-Agent" to "Mozilla/5.0"
        )
    }
}

internal class KugouLyricsProvider(
    private val client: OkHttpClient
) : LyricsProvider {
    override val source = LyricSource.KUGOU

    override suspend fun search(query: LyricQuery): List<LyricCandidate> {
        val keyword = urlEncode("${query.title} ${query.artist}")
        return parseKugouCandidates(
            client.getBody(
                "https://songsearch.kugou.com/song_search_v2?keyword=$keyword&page=1&pagesize=10&platform=WebFilter",
                kugouHeaders
            )
        )
    }

    override suspend fun fetch(candidate: LyricCandidate): RawLyrics {
        val searchBody = client.getBody(
            "https://krcs.kugou.com/search?keyword=%20-%20&ver=1&hash=${urlEncode(candidate.remoteId)}&client=mobi&man=yes",
            kugouHeaders
        )
        val lyricCandidate = parseKugouLyricCandidate(searchBody)
            ?: throw IOException("Kugou lyric candidate missing")
        val downloadBody = client.getBody(
            "https://lyrics.kugou.com/download?charset=utf8&accesskey=${urlEncode(lyricCandidate.second)}&id=${lyricCandidate.first}&client=mobi&fmt=lrc&ver=1",
            kugouHeaders
        )
        return parseKugouDownloadedLyrics(downloadBody)
    }

    private companion object {
        val kugouHeaders = mapOf(
            "User-Agent" to "IPhone-8990-searchSong",
            "UNI-UserAgent" to "iOS11.4-Phone8990-1009-0-WiFi"
        )
    }
}

internal fun parseNeteaseCandidates(body: String): List<LyricCandidate> {
    val songs = lyricsJson.parseToJsonElement(body).jsonObject["result"]
        ?.jsonObject?.get("songs")?.jsonArray.orEmpty()
    return songs.mapNotNull { element ->
        val song = element.jsonObject
        val id = song["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val title = song["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val artist = song["artists"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val duration = song["duration"]?.jsonPrimitive?.longOrNull ?: 0L
        LyricCandidate(LyricSource.NETEASE, id, title, artist, duration)
    }
}

internal fun parseNeteaseLyrics(body: String): RawLyrics {
    val root = lyricsJson.parseToJsonElement(body).jsonObject
    val yrc = nestedLyric(root, "yrc")
    val primary = if (!yrc.isNullOrBlank()) convertYrcToSpl(yrc) else nestedLyric(root, "lrc").orEmpty()
    val translation = (nestedLyric(root, "ytlrc") ?: nestedLyric(root, "tlyric"))
        ?.let(::convertYrcToSpl)
    val romanization = (nestedLyric(root, "yromalrc") ?: nestedLyric(root, "romalrc"))
        ?.let(::convertYrcToSpl)
    return RawLyrics(primary, translation, romanization)
}

internal fun parseQqCandidates(body: String): List<LyricCandidate> {
    val songNode = lyricsJson.parseToJsonElement(body).jsonObject["data"]?.jsonObject
        ?.get("song")?.jsonObject
    val songs = songNode?.get("list")?.jsonArray
        ?: songNode?.get("itemlist")?.jsonArray
        ?: emptyList()
    return songs.mapNotNull { element ->
        val song = element.jsonObject
        val id = song.stringValue("songmid", "mid") ?: return@mapNotNull null
        val title = song.stringValue("songname", "name") ?: return@mapNotNull null
        val artist = when (val singer = song["singer"]) {
            is JsonArray -> singer.firstOrNull()?.jsonObject
                ?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
            else -> singer?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        val durationSeconds = song["interval"]?.jsonPrimitive?.longOrNull ?: 0L
        LyricCandidate(LyricSource.QQ_MUSIC, id, title, artist, durationSeconds * 1_000L)
    }
}

internal fun parseQqLyrics(body: String): RawLyrics {
    val root = lyricsJson.parseToJsonElement(body).jsonObject
    return RawLyrics(
        primary = root["lyric"]?.jsonPrimitive?.contentOrNull.orEmpty().decodeHtmlEntities(),
        translation = root["trans"]?.jsonPrimitive?.contentOrNull?.decodeHtmlEntities()
    )
}

internal fun parseKugouCandidates(body: String): List<LyricCandidate> {
    val root = lyricsJson.parseToJsonElement(body).jsonObject
    if (root["status"]?.jsonPrimitive?.intOrNull != 1) return emptyList()
    val data = root["data"]?.jsonObject
    val songs = data?.get("info")?.jsonArray
        ?: data?.get("lists")?.jsonArray
        ?: emptyList()
    return songs.mapNotNull { element ->
        val song = element.jsonObject
        val id = song.stringValue("hash", "FileHash") ?: return@mapNotNull null
        val title = song.stringValue("songname", "SongName", "filename", "FileName")
            ?: return@mapNotNull null
        val artist = song.stringValue("singername", "SingerName").orEmpty()
        val durationSeconds = song.longValue("duration", "Duration") ?: 0L
        LyricCandidate(LyricSource.KUGOU, id, title, artist, durationSeconds * 1_000L)
    }
}

private fun JsonObject.stringValue(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    this[key]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.longValue(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
    this[key]?.jsonPrimitive?.longOrNull
}

internal fun parseKugouDownloadedLyrics(body: String): RawLyrics {
    val encoded = lyricsJson.parseToJsonElement(body).jsonObject["content"]
        ?.jsonPrimitive?.contentOrNull.orEmpty()
    val decoded = runCatching {
        String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
    }.getOrDefault("")
    return RawLyrics(decoded)
}

private fun parseKugouLyricCandidate(body: String): Pair<Long, String>? {
    val candidate = lyricsJson.parseToJsonElement(body).jsonObject["candidates"]
        ?.jsonArray?.firstOrNull()?.jsonObject ?: return null
    val id = candidate["id"]?.jsonPrimitive?.longOrNull ?: return null
    val accessKey = candidate["accesskey"]?.jsonPrimitive?.contentOrNull ?: return null
    return id to accessKey
}

private fun nestedLyric(root: JsonObject, key: String): String? {
    return root[key]?.jsonObject?.get("lyric")?.jsonPrimitive?.contentOrNull
}

private val yrcLinePattern = Regex("^\\[(\\d+),(\\d+)](.*)$")
private val yrcWordPattern = Regex("\\((\\d+),(\\d+),\\d+\\)([^()]*)")

private fun convertYrcToSpl(content: String): String {
    if (!content.contains(yrcWordPattern)) return content
    return content.lineSequence().mapNotNull { line ->
        val lineMatch = yrcLinePattern.matchEntire(line.trim()) ?: return@mapNotNull line
        val words = yrcWordPattern.findAll(lineMatch.groupValues[3]).toList()
        if (words.isEmpty()) return@mapNotNull null
        buildString {
            append(formatTimestamp(lineMatch.groupValues[1].toLong()))
            words.forEach { word ->
                val start = word.groupValues[1].toLong()
                append(formatSpanTimestamp(start))
                append(word.groupValues[3])
            }
            val last = words.last()
            append(formatSpanTimestamp(last.groupValues[1].toLong() + last.groupValues[2].toLong()))
        }
    }.joinToString("\n")
}

private fun formatTimestamp(timeMs: Long): String = "[%02d:%02d.%03d]".format(
    timeMs / 60_000L,
    (timeMs / 1_000L) % 60L,
    timeMs % 1_000L
)

private fun formatSpanTimestamp(timeMs: Long): String = "<%02d:%02d.%03d>".format(
    timeMs / 60_000L,
    (timeMs / 1_000L) % 60L,
    timeMs % 1_000L
)

private fun String.decodeHtmlEntities(): String = replace("&#58;", ":")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")

private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

private suspend fun OkHttpClient.getBody(
    url: String,
    headers: Map<String, String> = emptyMap()
): String {
    val request = Request.Builder().url(url).apply {
        headers.forEach { (name, value) -> header(name, value) }
    }.build()
    return newCall(request).awaitBody()
}

private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IOException("HTTP ${it.code}"))
                    }
                    return
                }
                if (continuation.isActive) continuation.resume(it.body.string())
            }
        }
    })
}
