package com.android.purebilibili.feature.audio.lyrics

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
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
        if (!forceRefresh) {
            cache.read(cacheKey)?.let { return LyricsLoadResult.Found(it) }
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
            val rankedCandidates = rankedCandidates(searchQueries, search.candidates)
                .filter { (_, score) -> score >= LYRIC_MATCH_MINIMUM_SCORE }
            if (rankedCandidates.isEmpty()) {
                return@withTimeoutOrNull LyricsLoadResult.NotFound
            }

            var fetchFailed = false
            for ((candidate, _) in rankedCandidates.take(5)) {
                val provider = providers.firstOrNull { it.source == candidate.source } ?: continue
                val fetched = runCatching {
                    withTimeout(providerTimeoutMs) { provider.fetch(candidate) }
                }
                if (fetched.isFailure) {
                    fetchFailed = true
                }
                val raw = fetched.getOrNull() ?: continue
                val document = parseSplLyrics(
                    primary = raw.primary,
                    translation = raw.translation,
                    romanization = raw.romanization,
                    source = candidate.source
                ).takeIf { it.lines.isNotEmpty() }
                    ?.copy(
                        remoteId = candidate.remoteId,
                        fetchedAtMs = nowMs()
                    )
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
        val raw = runCatching {
            withTimeout(providerTimeoutMs) { provider.fetch(providerCandidate) }
        }.getOrNull() ?: return LyricsLoadResult.Failed
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
                async {
                    runCatching {
                        withTimeout(providerTimeoutMs) { provider.search(query) }
                    }.getOrNull()
                }
                }
            }.awaitAll()
            ProviderSearchResult(
                candidates = results.filterNotNull().flatten(),
                completedProviderCount = results.count { it != null }
            )
        }
    }

    private fun rankedCandidates(
        queries: List<LyricQuery>,
        candidates: List<LyricCandidate>
    ): List<Pair<LyricCandidate, Double>> {
        val bestScores = linkedMapOf<String, Pair<LyricCandidate, Double>>()
        queries.forEach { query ->
            rankLyricCandidates(query, candidates).forEach { candidate ->
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
    val withoutVideoNoise = withoutPrefix.replace(
        Regex(
            "(?i)(?:4k|8k|\\d{3,4}p|hdr|official|music video|video|audio|lyrics?|lyric video|mv|live|cover|remix|官方|现场版|完整版|高音质|歌词版|片段|舞台版)"
        ),
        " "
    ).trim()
    val titleVariants = listOf(query.title, quotedTitle, withoutPrefix, withoutVideoNoise)
        .filterNotNull()
        .filter(String::isNotBlank)
        .distinct()
    val artistVariants = listOf(query.artist, extractedArtist)
        .filterNotNull()
        .filter(String::isNotBlank)
        .distinct()
    val generated = buildList {
        titleVariants.forEach { title ->
            artistVariants.forEach { artist -> add(query.copy(title = title, artist = artist)) }
        }
        add(query.copy(title = query.artist, artist = query.title))
        quotedTitle?.let { add(query.copy(title = query.artist, artist = it)) }
    }
    return generated
        .filter { it.title.isNotBlank() }
        .distinctBy { "${it.title.trim().lowercase()}\u0000${it.artist.trim().lowercase()}" }
        .take(8)
}

private fun lyricCandidateIdentity(candidate: LyricCandidate): String {
    val remoteId = candidate.remoteId.trim()
    return if (remoteId.isNotEmpty()) {
        "${candidate.source}:$remoteId"
    } else {
        "${candidate.source}:${candidate.title.trim().lowercase()}\u0000${candidate.artist.trim().lowercase()}\u0000${candidate.durationMs}"
    }
}
