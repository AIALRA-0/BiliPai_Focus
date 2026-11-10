package com.android.purebilibili.feature.audio.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

internal const val LYRIC_PROVIDER_TIMEOUT_MS = 5_000L
internal const val LYRIC_TOTAL_TIMEOUT_MS = 6_000L

internal data class RawLyrics(
    val primary: String,
    val translation: String? = null,
    val romanization: String? = null
)

internal interface LyricsProvider {
    val source: LyricSource

    suspend fun search(query: LyricQuery): List<LyricCandidate>

    suspend fun fetch(candidate: LyricCandidate): RawLyrics
}

internal interface LyricsCache {
    suspend fun read(key: String): LyricDocument?

    suspend fun write(key: String, document: LyricDocument)
}

internal sealed interface LyricsLoadResult {
    data class Found(val document: LyricDocument) : LyricsLoadResult
    data object NotFound : LyricsLoadResult
    data object Failed : LyricsLoadResult
}

private data class ProviderSearchResult(
    val candidates: List<LyricCandidate>,
    val completedProviderCount: Int
)

internal class LyricsRepository(
    private val providers: List<LyricsProvider>,
    private val cache: LyricsCache,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val providerTimeoutMs: Long = LYRIC_PROVIDER_TIMEOUT_MS,
    private val totalTimeoutMs: Long = LYRIC_TOTAL_TIMEOUT_MS
) {
    suspend fun load(
        cacheKey: String,
        query: LyricQuery,
        bilibiliLyrics: String?,
        forceRefresh: Boolean = false
    ): LyricsLoadResult {
        val cached = if (!forceRefresh) cache.read(cacheKey) else null
        currentCoroutineContext().ensureActive()
        if (cached != null && cachedLyricsMatchQuery(cached, query)) {
            return LyricsLoadResult.Found(cached)
        }

        bilibiliLyrics
            ?.takeIf { it.isNotBlank() }
            ?.let { parseSplLyrics(it, source = LyricSource.BILIBILI) }
            ?.takeIf { it.lines.isNotEmpty() }
            ?.let { document ->
                val cached = document.copy(fetchedAtMs = nowMs())
                cache.write(cacheKey, cached)
                return LyricsLoadResult.Found(cached)
            }

        val attempt = withTimeoutOrNull(totalTimeoutMs) {
            val searchQueries = automaticSearchQueries(query)
            val search = searchAllProviders(searchQueries)
            if (search.completedProviderCount == 0 && providers.isNotEmpty()) {
                return@withTimeoutOrNull LyricsLoadResult.Failed
            }
            val rankedCandidates = rankedCandidates(searchQueries, search.candidates, automatic = true)
                .filter { (_, score) -> score >= LYRIC_MATCH_MINIMUM_SCORE }
            if (rankedCandidates.isEmpty()) {
                return@withTimeoutOrNull LyricsLoadResult.NotFound
            }

            var fetchFailed = false
            for ((candidate, _) in rankedCandidates.take(5)) {
                val provider = providers.firstOrNull { it.source == candidate.source } ?: continue
                val raw = providerRequest { provider.fetch(candidate) }
                if (raw == null) {
                    fetchFailed = true
                    continue
                }
                val document = parseSplLyrics(
                    primary = raw.primary,
                    translation = raw.translation,
                    romanization = raw.romanization,
                    source = candidate.source
                ).takeIf { it.lines.isNotEmpty() }
                    ?.let { parsed ->
                        parsed.copy(
                            remoteId = candidate.remoteId,
                            fetchedAtMs = nowMs(),
                            offsetMs = cached?.offsetMs ?: 0L,
                            metadata = parsed.metadata + lyricMatchCacheMetadata(query),
                        )
                    }
                if (document != null) {
                    return@withTimeoutOrNull LyricsLoadResult.Found(document)
                }
            }
            if (fetchFailed) LyricsLoadResult.Failed else LyricsLoadResult.NotFound
        }

        if (attempt is LyricsLoadResult.Found) {
            cache.write(cacheKey, attempt.document)
        }
        return attempt ?: LyricsLoadResult.Failed
    }

    suspend fun search(query: LyricQuery): List<LyricCandidate> {
        return withTimeoutOrNull(totalTimeoutMs) {
            val queries = automaticSearchQueries(query)
            val search = searchAllProviders(queries)
            rankedCandidates(queries, search.candidates).map { it.first }
        }.orEmpty()
    }

    suspend fun save(cacheKey: String, document: LyricDocument) {
        cache.write(cacheKey, document)
    }

    suspend fun select(
        cacheKey: String,
        providerCandidate: LyricCandidate
    ): LyricsLoadResult {
        val provider = providers.firstOrNull { it.source == providerCandidate.source }
            ?: return LyricsLoadResult.NotFound
        val raw = providerRequest { provider.fetch(providerCandidate) } ?: return LyricsLoadResult.Failed
        val document = parseSplLyrics(
            primary = raw.primary,
            translation = raw.translation,
            romanization = raw.romanization,
            source = providerCandidate.source
        ).copy(
            remoteId = providerCandidate.remoteId,
            manuallySelected = true,
            fetchedAtMs = nowMs()
        )
        if (document.lines.isEmpty()) return LyricsLoadResult.NotFound
        cache.write(cacheKey, document)
        return LyricsLoadResult.Found(document)
    }

    private suspend fun searchAllProviders(queries: List<LyricQuery>): ProviderSearchResult {
        return supervisorScope {
            val results = providers.flatMap { provider ->
                queries.map { query ->
                    async { providerRequest { provider.search(query) } }
                }
            }.awaitAll()
            ProviderSearchResult(
                candidates = results.filterNotNull().flatten(),
                completedProviderCount = results.count { it != null }
            )
        }
    }

    private suspend fun <T> providerRequest(block: suspend () -> T): T? = try {
        withTimeoutOrNull(providerTimeoutMs) { block() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun rankedCandidates(
        queries: List<LyricQuery>,
        candidates: List<LyricCandidate>,
        automatic: Boolean = false,
    ): List<Pair<LyricCandidate, Double>> {
        val bestScores = linkedMapOf<String, Pair<LyricCandidate, Double>>()
        queries.forEach { query ->
            rankLyricCandidates(
                query,
                candidates,
                durationToleranceMs = if (automatic) LYRIC_MATCH_DURATION_TOLERANCE_MS else Long.MAX_VALUE,
            )
                .filter { !automatic || isAutomaticLyricCandidateAllowed(query, it) }
                .forEach { candidate ->
                    val score = scoreLyricCandidate(query, candidate)
                    val key = lyricCandidateIdentity(candidate)
                    val previous = bestScores[key]
                    if (previous == null || score > previous.second) {
                        bestScores[key] = candidate to score
                    }
                }
        }
        return bestScores.values.sortedByDescending { it.second }
    }
}

private fun lyricMatchCacheMetadata(query: LyricQuery): Map<String, String> = mapOf(
    "match_policy" to "2",
    "match_title" to query.title,
    "match_artist" to query.artist,
    "match_original_title" to query.originalTitle,
    "match_duration_ms" to query.durationMs.toString(),
    "match_artist_is_uploader" to query.artistIsUploader.toString(),
)

/** 自动缓存需与当前检索信息一致；手动选择和原生歌词不因匹配策略升级丢失。 */
private fun cachedLyricsMatchQuery(document: LyricDocument, query: LyricQuery): Boolean {
    if (document.manuallySelected || document.source == LyricSource.MANUAL || document.source == LyricSource.BILIBILI) return true
    return lyricMatchCacheMetadata(query).all { (key, value) -> document.metadata[key] == value }
}

private fun automaticSearchQueries(query: LyricQuery): List<LyricQuery> {
    val quotedTitle = Regex("[《「『]([^》」』]+)[》」』]")
        .find(query.title)
        ?.groupValues
        ?.getOrNull(1)
    val extractedArtist = Regex("([\\p{L}\\p{N}·][\\p{L}\\p{N}· _-]{1,24})\\s*[《「『]")
        .find(query.title)
        ?.groupValues
        ?.getOrNull(1)
    val withoutPrefix = query.title.replace(Regex("^(?:【[^】]*】|\\[[^]]*])+"), "").trim()
    // 去掉文件名开头的音轨序号，如 "004.周杰伦-七里香" / "01 - 七里香"
    val withoutTrackNumber = withoutPrefix
        .replace(Regex("^(?:\\d{1,4}\\s*[.、·_－-]\\s*|\\d{1,4}\\s)+"), "")
        .trim()
    val withoutVideoNoise = stripLyricVideoNoise(withoutTrackNumber).trim()
    val titleVariants = listOf(
        query.title,
        quotedTitle,
        withoutPrefix,
        withoutTrackNumber,
        withoutVideoNoise,
    )
        .filterNotNull()
        .filter(String::isNotBlank)
        .distinct()
    val artistVariants = listOf(query.artist, extractedArtist)
        .filterNotNull()
        .filter(String::isNotBlank)
        .distinct()
    // "周杰伦-七里香" / "周杰伦 - 七里香"：把连字符两侧当作 歌手-歌名 拆出
    val dashSegments = withoutTrackNumber
        .split(Regex("\\s*[-–—]\\s*"))
        .map(String::trim)
        .filter { it.isNotEmpty() && it.length <= 30 }
    val generated = buildList {
        titleVariants.forEach { title ->
            artistVariants.forEach { artist -> add(query.copy(title = title, artist = artist)) }
        }
        add(query.copy(title = query.artist, artist = query.title))
        quotedTitle?.let { add(query.copy(title = query.artist, artist = it)) }
        if (dashSegments.size >= 2) {
            val maybeArtist = dashSegments.first()
            val maybeTitle = dashSegments.last()
            if (maybeTitle != maybeArtist) {
                add(query.copy(title = maybeTitle, artist = maybeArtist))
                artistVariants.forEach { artist ->
                    add(query.copy(title = maybeTitle, artist = artist))
                }
            }
        }
    }
    return generated
        .filter { it.title.isNotBlank() }
        .distinctBy { "${it.title.trim().lowercase()}\u0000${it.artist.trim().lowercase()}" }
        .take(12)
}

private fun lyricCandidateIdentity(candidate: LyricCandidate): String {
    val remoteId = candidate.remoteId.trim()
    return if (remoteId.isNotEmpty()) {
        "${candidate.source}:$remoteId"
    } else {
        "${candidate.source}:${candidate.title.trim().lowercase()}\u0000${candidate.artist.trim().lowercase()}\u0000${candidate.durationMs}"
    }
}
