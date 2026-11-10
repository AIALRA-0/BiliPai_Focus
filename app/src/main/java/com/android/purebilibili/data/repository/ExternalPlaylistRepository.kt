// File: data/repository/ExternalPlaylistRepository.kt
package com.android.purebilibili.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.settingsDataStore
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** 外部歌单（网易云 / QQ 音乐）导入与匹配。 */
object ExternalPlaylistRepository {

    enum class Source(val label: String) {
        NETEASE("网易云歌单"),
        QQ("QQ音乐歌单")
    }

    @Serializable
    data class ExternalTrack(
        val title: String,
        val artists: List<String> = emptyList(),
        val album: String = "",
        val durationMs: Long = 0L,
        val coverUrl: String = "",
        val translatedTitle: String? = null
    )

    @Serializable
    data class ExternalPlaylistMeta(
        val source: Source,
        val playlistId: String,
        val name: String,
        val coverUrl: String = "",
        val author: String = "",
        val tracks: List<ExternalTrack> = emptyList()
    )

    @Serializable
    data class MatchedVideo(
        val bvid: String,
        val title: String,
        val cover: String = "",
        val author: String = "",
        val durationSec: Long = 0L,
        /** 搜索结果中的 aid，收藏到 B 站收藏夹时使用；旧断点可能为 0。 */
        val aid: Long = 0L,
    )

    data class MatchOutcome(
        val track: ExternalTrack,
        val video: MatchedVideo?
    )

    data class ImportCheckpoint(
        val playlist: ExternalPlaylistMeta,
        val outcomes: List<MatchOutcome>,
        val completedCount: Int,
    )

    @Serializable
    private data class SerializableMatchOutcome(
        val track: ExternalTrack,
        val video: MatchedVideo? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val importCheckpointKey = stringPreferencesKey("external_playlist_import_checkpoint_v1")

    suspend fun loadImportCheckpoint(context: Context): ImportCheckpoint? = runCatching {
        val raw = context.settingsDataStore.data.first()[importCheckpointKey] ?: return null
        val stored = json.decodeFromString<StoredImportCheckpoint>(raw)
        ImportCheckpoint(
            playlist = stored.playlist,
            outcomes = stored.outcomes.map { MatchOutcome(it.track, it.video) },
            completedCount = stored.completedCount.coerceIn(0, stored.playlist.tracks.size),
        )
    }.getOrNull()

    suspend fun saveImportCheckpoint(context: Context, checkpoint: ImportCheckpoint) {
        val stored = StoredImportCheckpoint(
            playlist = checkpoint.playlist,
            outcomes = checkpoint.outcomes.map { SerializableMatchOutcome(it.track, it.video) },
            completedCount = checkpoint.completedCount.coerceIn(0, checkpoint.playlist.tracks.size),
        )
        context.settingsDataStore.edit { it[importCheckpointKey] = json.encodeToString(stored) }
    }

    suspend fun clearImportCheckpoint(context: Context) {
        context.settingsDataStore.edit { it.remove(importCheckpointKey) }
    }

    @Serializable
    private data class StoredImportCheckpoint(
        val playlist: ExternalPlaylistMeta,
        val outcomes: List<SerializableMatchOutcome>,
        val completedCount: Int,
    )

    // 匹配过滤规则：音MAD/现场/翻唱/科普/运动分区一律排除，避免匹配到非音乐内容
    private val BLACKLIST_ZONES = setOf(26, 29, 31, 201, 238)
    private const val DURATION_TOLERANCE_SEC = 20L
    private const val MATCH_DELAY_MS = 1200L
    private const val NETEASE_WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val NETEASE_SONG_DETAIL_CHUNK_SIZE = 200
    private const val NETEASE_MIRROR_BASE_URL = "https://nextmusic.toubiec.cn"

    /** 镜像接口可能不可用，单独限制总超时，避免阻塞官方接口的回退。 */
    private val neteaseMirrorClient by lazy {
        NetworkModule.okHttpClient.newBuilder()
            .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // ---------------------------------------------------------------- 输入解析

    /** 从分享链接或纯数字 id 解析 (来源, 歌单 id)；来源无法识别时返回 null。 */
    fun parsePlaylistInput(input: String): Pair<Source, String>? {
        val text = input.trim()
        if (text.isEmpty()) return null
        return when {
            text.contains("163.com") || text.contains("music.163") -> {
                extractId(text, listOf("id=", "/playlist/", "#/playlist"))?.let { Source.NETEASE to it }
                    ?: text.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.let { Source.NETEASE to it }
            }
            text.contains("qq.com") || text.contains("y.qq.com") -> {
                extractId(text, listOf("dissid=", "id=", "/playlist/"))?.let { Source.QQ to it }
                    ?: text.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.let { Source.QQ to it }
            }
            text.matches(Regex("\\d{4,}")) -> null // 纯数字无法判断来源，交给调用方指定
            else -> null
        }
    }

    private fun extractId(text: String, markers: List<String>): String? {
        for (marker in markers) {
            val index = text.indexOf(marker)
            if (index < 0) continue
            val rest = text.substring(index + marker.length)
            val id = rest.takeWhile { it.isDigit() }
            if (id.isNotEmpty()) return id
        }
        return null
    }

    // ---------------------------------------------------------------- 歌单拉取

    suspend fun fetchPlaylist(
        source: Source,
        playlistId: String,
        explicitId: String? = null
    ): Result<ExternalPlaylistMeta> = withContext(Dispatchers.IO) {
        val id = explicitId ?: playlistId
        if (id.isBlank()) return@withContext Result.failure(IllegalArgumentException("歌单 id 不能为空"))
        when (source) {
            Source.QQ -> fetchQqPlaylist(id)
            Source.NETEASE -> fetchNeteasePlaylist(id)
        }
    }

    private fun fetchQqPlaylist(id: String): Result<ExternalPlaylistMeta> = runCatching {
        val url = "https://c.y.qq.com/v8/fcg-bin/fcg_v8_playlist_cp.fcg" +
            "?id=$id&format=json&newsong=1&platform=jqspaframe.json"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Referer", "https://y.qq.com")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
            )
            .build()
        NetworkModule.okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val root = json.parseToJsonElement(body).jsonObject
            val cd = root["data"]?.jsonObject?.get("cdlist")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?: throw IllegalStateException("未找到该 QQ 音乐歌单")
            val songlist = cd["songlist"]?.jsonArray.orEmpty()
            val tracks = songlist.mapNotNull { element ->
                val song = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                val title = song["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (title.isBlank()) return@mapNotNull null
                ExternalTrack(
                    title = title,
                    artists = song["singer"]?.jsonArray.orEmpty()
                        .mapNotNull { runCatching { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.getOrNull() },
                    album = song["album"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty(),
                    durationMs = (song["interval"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L) * 1000L,
                    coverUrl = song["album"]?.jsonObject?.get("mid")?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "https://y.gtimg.cn/music/photo_new/T002R300x300M000$it.jpg" }
                        .orEmpty(),
                    translatedTitle = song["subtitle"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                )
            }
            ExternalPlaylistMeta(
                source = Source.QQ,
                playlistId = id,
                name = cd["dissname"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "QQ音乐歌单" },
                coverUrl = cd["logo"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                author = cd["nickname"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                tracks = tracks
            )
        }
    }.onFailure { Logger.w("ExternalPlaylist", "fetch QQ playlist failed: ${it.message}") }

    private fun fetchNeteasePlaylist(id: String): Result<ExternalPlaylistMeta> = runCatching {
        if (!id.matches(Regex("\\d+"))) {
            throw IllegalStateException("网易云歌单 id 格式不正确")
        }
        // 正常导入优先使用官方接口，只有失败时才访问备用镜像。
        val official = fetchNeteasePlaylistFromWeb(id)
        official.getOrNull()?.let { return@runCatching it }
        Logger.w("ExternalPlaylist", "netease web api failed, trying mirror: ${official.exceptionOrNull()?.message}")
        val mirror = fetchNeteasePlaylistFromMirror(id)
        mirror.getOrNull()?.takeIf { it.tracks.isNotEmpty() }?.let { return@runCatching it }
        // 镜像也失败时保留官方错误，避免将故障替换为无关解析异常。
        throw official.exceptionOrNull() ?: IllegalStateException("网易云歌单导入失败")
    }.onFailure { Logger.w("ExternalPlaylist", "fetch netease playlist failed: ${it.message}") }

    /**
     * 备用镜像先获取出口 IP，再请求整张歌单；两次调用共用 10 秒等待预算。
     * 该接口会校验 User-Agent，必须使用浏览器 UA。
     */
    private fun fetchNeteasePlaylistFromMirror(id: String): Result<ExternalPlaylistMeta> = runCatching {
        val deadlineNanos = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10)
        val ip = fetchNeteaseMirrorClientIp(deadlineNanos)
            ?: throw IllegalStateException("网易云镜像接口获取客户端 IP 失败")
        val payload = """{"id":"$id","ip":"$ip","timestamp":${System.currentTimeMillis()}}"""
        val request = okhttp3.Request.Builder()
            .url("$NETEASE_MIRROR_BASE_URL/api/playlist_trackall")
            .header("Content-Type", "application/json")
            .header("Origin", "https://wyapi.toubiec.cn")
            .header("Referer", "https://wyapi.toubiec.cn/")
            .header("User-Agent", NETEASE_WEB_USER_AGENT)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        neteaseMirrorCall(request, deadlineNanos).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IllegalStateException("网易云镜像接口返回空响应（HTTP ${response.code}）")
            }
            val root = json.parseToJsonElement(body).jsonObject
            val code = root["code"]?.jsonPrimitive?.intOrNull
            if (code != 200) {
                throw IllegalStateException(
                    root["message"]?.jsonPrimitive?.contentOrNull ?: "网易云镜像接口返回错误码 $code"
                )
            }
            val data = root["data"]?.jsonObject
                ?: throw IllegalStateException("未找到该网易云歌单")
            val tracks = data["songs"]?.jsonArray.orEmpty().mapNotNull { element ->
                val song = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                val title = song["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (title.isBlank()) return@mapNotNull null
                ExternalTrack(
                    title = title,
                    artists = song["singer"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        .split('/')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                    album = song["album"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    durationMs = parseNeteaseDurationText(song["duration"]?.jsonPrimitive?.contentOrNull),
                    coverUrl = song["picimg"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        .replace("http://", "https://")
                )
            }
            ExternalPlaylistMeta(
                source = Source.NETEASE,
                playlistId = id,
                name = data["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "网易云歌单" },
                coverUrl = data["coverImage"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    .replace("http://", "https://"),
                author = data["creator"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty(),
                tracks = tracks
            )
        }
    }

    private fun neteaseMirrorCall(request: okhttp3.Request, deadlineNanos: Long): okhttp3.Call {
        val remainingNanos = deadlineNanos - System.nanoTime()
        if (remainingNanos <= 0L) throw java.io.InterruptedIOException("网易云备用接口等待超时")
        return neteaseMirrorClient.newCall(request).apply {
            timeout().timeout(remainingNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
        }
    }

    private fun fetchNeteaseMirrorClientIp(deadlineNanos: Long): String? {
        val payload = """{"timestamp":${System.currentTimeMillis()}}"""
        val request = okhttp3.Request.Builder()
            .url("$NETEASE_MIRROR_BASE_URL/api/ip")
            .header("Content-Type", "application/json")
            .header("Origin", "https://wyapi.toubiec.cn")
            .header("Referer", "https://wyapi.toubiec.cn/")
            .header("User-Agent", NETEASE_WEB_USER_AGENT)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            neteaseMirrorCall(request, deadlineNanos).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                val root = json.parseToJsonElement(body).jsonObject
                if (root["code"]?.jsonPrimitive?.intOrNull != 200) return@use null
                root["data"]?.jsonObject?.get("ip")?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    /** 镜像接口的时长格式为 "m:ss"（超长音频可能是 "h:mm:ss"）。 */
    private fun parseNeteaseDurationText(text: String?): Long {
        val parts = text?.split(':')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty()
        if (parts.isEmpty()) return 0L
        return parts.fold(0L) { total, part -> total * 60 + part } * 1000L
    }

    /**
     * 匿名访问网易云 v6 歌单详情时，返回的 tracks 只有前几首，但 trackIds 是完整的。
     * 因此先取 trackIds，再分批请求 v3 song/detail 拿到全部曲目。
     *
     * 旧实现走 eapi（interface3.music.163.com），服务端现在会返回 200 + 空 body，
     * 导致 kotlinx.serialization 抛出 "unexpected end of the input at path: $"。
     */
    private fun fetchNeteasePlaylistFromWeb(id: String): Result<ExternalPlaylistMeta> = runCatching {
        val detailRequest = okhttp3.Request.Builder()
            .url("https://music.163.com/api/v6/playlist/detail?id=$id")
            .header("Referer", "https://music.163.com")
            .header("User-Agent", NETEASE_WEB_USER_AGENT)
            .build()
        NetworkModule.okHttpClient.newCall(detailRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IllegalStateException("网易云歌单接口返回空响应（HTTP ${response.code}）")
            }
            val root = json.parseToJsonElement(body).jsonObject
            val code = root["code"]?.jsonPrimitive?.intOrNull
            if (code != 200 && code != 0) throw IllegalStateException("网易云接口返回错误码 $code")
            val playlist = root["playlist"]?.jsonObject
                ?: throw IllegalStateException("未找到该网易云歌单")

            val trackIds = playlist["trackIds"]?.jsonArray.orEmpty().mapNotNull { element ->
                runCatching { element.jsonObject["id"]?.jsonPrimitive?.longOrNull }.getOrNull()
            }
            val songs = if (trackIds.isNotEmpty()) {
                fetchNeteaseSongs(trackIds)
            } else {
                // 兜底：某些接口变体不返回 trackIds，只能使用 tracks 字段。
                playlist["tracks"]?.jsonArray.orEmpty().mapNotNull { element ->
                    runCatching { element.jsonObject }.getOrNull()
                }
            }
            val tracks = songs.mapNotNull { parseNeteaseTrack(it) }

            ExternalPlaylistMeta(
                source = Source.NETEASE,
                playlistId = id,
                name = playlist["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "网易云歌单" },
                coverUrl = playlist["coverImgUrl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                author = playlist["creator"]?.jsonObject?.get("nickname")?.jsonPrimitive?.contentOrNull.orEmpty(),
                tracks = tracks
            )
        }
    }.onFailure { Logger.w("ExternalPlaylist", "fetch netease playlist from web failed: ${it.message}") }

    /** 按 trackIds 顺序批量获取歌曲详情；分块避免单次请求参数过长。 */
    private fun fetchNeteaseSongs(ids: List<Long>): List<JsonObject> {
        val songsById = LinkedHashMap<Long, JsonObject>()
        ids.chunked(NETEASE_SONG_DETAIL_CHUNK_SIZE).forEach { chunk ->
            val payload = chunk.joinToString(prefix = "[", postfix = "]") { """{"id":$it}""" }
            val request = okhttp3.Request.Builder()
                .url("https://music.163.com/api/v3/song/detail")
                .header("Referer", "https://music.163.com")
                .header("User-Agent", NETEASE_WEB_USER_AGENT)
                .post(okhttp3.FormBody.Builder().add("c", payload).build())
                .build()
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    throw IllegalStateException("网易云歌曲接口返回空响应（HTTP ${response.code}）")
                }
                val root = json.parseToJsonElement(body).jsonObject
                val code = root["code"]?.jsonPrimitive?.intOrNull
                if (code != 200 && code != 0) {
                    throw IllegalStateException("网易云歌曲接口返回错误码 $code")
                }
                root["songs"]?.jsonArray.orEmpty().forEach { element ->
                    val song = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
                    val songId = song["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                    songsById[songId] = song
                }
            }
        }
        return ids.mapNotNull { songsById[it] }
    }

    private fun parseNeteaseTrack(track: JsonObject): ExternalTrack? {
        val title = track["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (title.isBlank()) return null
        return ExternalTrack(
            title = title,
            artists = track["ar"]?.jsonArray.orEmpty()
                .mapNotNull { runCatching { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.getOrNull() },
            album = track["al"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty(),
            durationMs = track["dt"]?.jsonPrimitive?.longOrNull ?: 0L,
            coverUrl = track["al"]?.jsonObject?.get("picUrl")?.jsonPrimitive?.contentOrNull
                ?.replace("http://", "https://").orEmpty(),
            translatedTitle = track["tns"]?.jsonArray?.firstOrNull()
                ?.jsonPrimitive?.contentOrNull
        )
    }

    // ---------------------------------------------------------------- B站匹配

    /**
     * 在 B 站搜索一首歌并返回最佳匹配。
     * 策略：黑名单分区过滤 → 时长差 ≤20s → 取首个通过项（搜索排序已含相关性）。
     */
    suspend fun matchTrack(track: ExternalTrack): MatchOutcome = withContext(Dispatchers.IO) {
        val query = buildString {
            append(track.title)
            track.translatedTitle?.takeIf { it.isNotBlank() }?.let { append(' ').append(it) }
            val artists = track.artists.joinToString(" ")
            if (artists.isNotBlank()) append(" - ").append(artists)
        }
        val video = runCatching {
            val (items, _) = SearchRepository.search(keyword = query, page = 1)
                .getOrElse { return@withContext MatchOutcome(track, null) }
            items.firstOrNull { candidate ->
                candidate.tid !in BLACKLIST_ZONES &&
                    candidate.duration in 1..Int.MAX_VALUE &&
                    track.durationMs > 0L &&
                    kotlin.math.abs(candidate.duration.toLong() - track.durationMs / 1000L) <= DURATION_TOLERANCE_SEC
            }?.let {
                MatchedVideo(
                    bvid = it.bvid,
                    title = it.title,
                    cover = it.pic,
                    author = it.owner.name,
                    durationSec = it.duration.toLong(),
                    aid = it.id,
                )
            }
        }.getOrNull()
        MatchOutcome(track, video)
    }

    suspend fun matchTracks(
        tracks: List<ExternalTrack>,
        startIndex: Int = 0,
        onProgress: suspend (completed: Int, total: Int, outcome: MatchOutcome) -> Unit
    ): List<MatchOutcome> {
        val results = mutableListOf<MatchOutcome>()
        tracks.forEachIndexed { index, track ->
            if (index < startIndex) return@forEachIndexed
            val outcome = matchTrack(track)
            results += outcome
            onProgress(index + 1, tracks.size, outcome)
            if (index != tracks.lastIndex) kotlinx.coroutines.delay(MATCH_DELAY_MS)
        }
        return results
    }

    fun buildSearchQueryForManualMatch(track: ExternalTrack): String {
        return buildString {
            append(track.title)
            val artists = track.artists.joinToString(" ")
            if (artists.isNotBlank()) append(' ').append(artists)
        }
    }
}
