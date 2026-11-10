package com.android.purebilibili.feature.audio.lyrics

import java.text.Normalizer
import kotlin.math.abs

internal const val LYRIC_MATCH_MINIMUM_SCORE = 0.72
internal const val LYRIC_MATCH_DURATION_TOLERANCE_MS = 8_000L

private const val LYRIC_MATCH_STRONG_METADATA_DURATION_TOLERANCE_MS = 30_000L

private data class LyricCandidateMatch(
    val candidate: LyricCandidate,
    val titleScore: Double,
    val artistScore: Double,
    val durationScore: Double,
    val score: Double
)

internal fun scoreLyricCandidate(
    query: LyricQuery,
    candidate: LyricCandidate
): Double = matchLyricCandidate(query, candidate).score

/**
 * Returns candidates in the order in which they should be presented or fetched.
 * Providers often return popularity order, which is not a reliable match order
 * when a video title contains prefixes, live/version suffixes, or features.
 */
internal fun rankLyricCandidates(
    query: LyricQuery,
    candidates: List<LyricCandidate>,
    durationToleranceMs: Long = LYRIC_MATCH_DURATION_TOLERANCE_MS
): List<LyricCandidate> {
    return candidates
        .asSequence()
        .map { matchLyricCandidate(query, it) }
        .filter { match ->
            isDurationAllowed(
                query = query,
                candidate = match.candidate,
                titleScore = match.titleScore,
                artistScore = match.artistScore,
                durationToleranceMs = durationToleranceMs
            )
        }
        .sortedWith(
            compareByDescending<LyricCandidateMatch> { it.score }
                .thenBy { providerPriority(it.candidate.source) }
                .thenBy {
                    if (query.durationMs > 0L && it.candidate.durationMs > 0L) {
                        abs(query.durationMs - it.candidate.durationMs)
                    } else {
                        Long.MAX_VALUE
                    }
                }
        )
        .map { it.candidate }
        .toList()
}

internal fun selectBestLyricCandidate(
    query: LyricQuery,
    candidates: List<LyricCandidate>,
    minimumScore: Double = LYRIC_MATCH_MINIMUM_SCORE,
    durationToleranceMs: Long = LYRIC_MATCH_DURATION_TOLERANCE_MS
): LyricCandidate? {
    return rankLyricCandidates(query, candidates, durationToleranceMs)
        .firstOrNull { isAutomaticLyricCandidateAllowed(query, it, minimumScore, durationToleranceMs) }
}

/** 自动采用要求版本一致；UP 名称不是已确认的演唱者，缺少歌手证据时收紧时长条件。 */
internal fun isAutomaticLyricCandidateAllowed(
    query: LyricQuery,
    candidate: LyricCandidate,
    minimumScore: Double = LYRIC_MATCH_MINIMUM_SCORE,
    durationToleranceMs: Long = LYRIC_MATCH_DURATION_TOLERANCE_MS,
): Boolean {
    if (!lyricVersionsMatch(query.originalTitle, candidate.title)) return false
    if (query.durationMs > 0L && candidate.durationMs > 0L &&
        abs(query.durationMs - candidate.durationMs) > durationToleranceMs
    ) return false
    val match = matchLyricCandidate(query, candidate)
    if (match.score < minimumScore || match.titleScore < 0.72) return false
    val performerKnown = (!query.artistIsUploader && query.artist.isNotBlank()) ||
        extractPerformerVariants(query.originalTitle).isNotEmpty()
    if (performerKnown) return match.artistScore >= 0.50
    return match.titleScore >= 0.90 && query.durationMs > 0L && candidate.durationMs > 0L &&
        abs(query.durationMs - candidate.durationMs) <= durationToleranceMs
}

private val lyricVersionPatterns = listOf(
    Regex("\\blive\\b|现场|演唱会|舞台版", RegexOption.IGNORE_CASE),
    Regex("\\bcover\\b|翻唱|翻自", RegexOption.IGNORE_CASE),
    Regex("\\bremix\\b|混音", RegexOption.IGNORE_CASE),
    Regex("\\binstrumental\\b|\\bkaraoke\\b|伴奏|纯音乐", RegexOption.IGNORE_CASE),
    Regex("\\bsped\\s*up\\b|加速版|升速版", RegexOption.IGNORE_CASE),
    Regex("\\bslowed\\b|降速版|慢速版", RegexOption.IGNORE_CASE),
)

internal fun lyricVersionsMatch(left: String, right: String): Boolean {
    fun versions(value: String): Set<Int> = lyricVersionPatterns
        .mapIndexedNotNull { index, pattern -> index.takeIf { pattern.containsMatchIn(value) } }.toSet()
    return versions(left) == versions(right)
}

/** 行内容必须有相似性，时间靠近本身不足以证明是同一句歌词。 */
internal fun lyricLinesMatch(left: String, right: String): Boolean {
    val normalizedLeft = normalizeLyricMatchText(left)
    val normalizedRight = normalizeLyricMatchText(right)
    return normalizedLeft.isNotBlank() && normalizedRight.isNotBlank() &&
        stringSimilarity(normalizedLeft, normalizedRight) >= 0.50
}

private fun matchLyricCandidate(
    query: LyricQuery,
    candidate: LyricCandidate
): LyricCandidateMatch {
    val titleScore = maximumVariantSimilarity(
        lyricTitleMatchVariants(query.title),
        lyricTitleMatchVariants(candidate.title)
    )
    val queryArtistVariants = buildList {
        addAll(lyricArtistMatchVariants(query.artist))
        addAll(extractPerformerVariants(query.originalTitle))
    }.filter(String::isNotBlank).distinct()
    val artistScore = maximumVariantSimilarity(
        queryArtistVariants,
        lyricArtistMatchVariants(candidate.artist)
    )
    val durationScore = lyricDurationScore(
        query = query,
        candidate = candidate,
        strongMetadata = titleScore >= 0.86 && artistScore >= 0.72
    )
    return LyricCandidateMatch(
        candidate = candidate,
        titleScore = titleScore,
        artistScore = artistScore,
        durationScore = durationScore,
        score = (titleScore * 0.55) + (artistScore * 0.25) + (durationScore * 0.20)
    )
}

private fun lyricTitleMatchVariants(value: String): List<String> {
    val quoted = listOf(
        Regex("《([^》]+)》"),
        Regex("「([^」]+)」"),
        Regex("『([^』]+)』"),
        Regex("[“\"]([^”\"]+)[”\"]")
    ).flatMap { pattern ->
        pattern.findAll(value).map { match ->
            normalizeLyricMatchText(stripLyricVideoNoise(match.groupValues[1]), removeBracketedPrefix = true)
        }.toList()
    }
    val cleaned = stripLyricVideoNoise(value)
    val separatorParts = cleaned.split(Regex("\\s+[-|｜]\\s+"))
    return (quoted + listOf(value, cleaned) + separatorParts).map {
        normalizeLyricMatchText(it, removeBracketedPrefix = true)
    }
        .filter(String::isNotBlank)
        .distinct()
}

private fun lyricArtistMatchVariants(value: String): List<String> {
    val cleaned = stripLyricVideoNoise(value)
    val parts = cleaned.split(
        Regex("(?i)\\s*(?:[,，、/&+·;；]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)\\s*")
    )
    return (listOf(value, cleaned) + parts)
        .map(::normalizeLyricMatchText)
        .filter(String::isNotBlank)
        .distinct()
}

private fun extractPerformerVariants(value: String): List<String> {
    return Regex("([\\p{L}\\p{N}·][\\p{L}\\p{N}· _-]{1,24})\\s*[《「『]")
        .findAll(value)
        .map { match -> normalizeLyricMatchText(match.groupValues[1]) }
        .filter(String::isNotBlank)
        .toList()
}

private val lyricVideoNoisePattern = Regex(
    "(?i)(?:\\b(?:4k|8k|\\d{3,4}p|hdr|official|music video|lyric video|video|audio|lyrics?|mv|live|cover|remix|karaoke|instrumental|piano|flac|hi-res)\\b|官方|现场版|完整版|高音质|歌词版|片段|舞台版|纯音乐|伴奏版|伴奏|卡拉ok|独奏|翻唱|自存|无损|重制版|翻自)"
)

internal fun stripLyricVideoNoise(value: String): String = value.replace(lyricVideoNoisePattern, " ")

private fun maximumVariantSimilarity(
    leftVariants: List<String>,
    rightVariants: List<String>
): Double {
    return leftVariants.maxOfOrNull { left ->
        rightVariants.maxOfOrNull { right -> stringSimilarity(left, right) } ?: 0.0
    } ?: 0.0
}

private fun lyricDurationScore(
    query: LyricQuery,
    candidate: LyricCandidate,
    strongMetadata: Boolean
): Double {
    if (query.durationMs <= 0L || candidate.durationMs <= 0L) return 0.5
    val differenceMs = abs(query.durationMs - candidate.durationMs)
    return when {
        differenceMs <= LYRIC_MATCH_DURATION_TOLERANCE_MS -> {
            1.0 - differenceMs.toDouble() / LYRIC_MATCH_DURATION_TOLERANCE_MS.toDouble()
        }
        strongMetadata && differenceMs <= LYRIC_MATCH_STRONG_METADATA_DURATION_TOLERANCE_MS -> {
            0.35 * (
                1.0 - (differenceMs - LYRIC_MATCH_DURATION_TOLERANCE_MS).toDouble() /
                    (LYRIC_MATCH_STRONG_METADATA_DURATION_TOLERANCE_MS - LYRIC_MATCH_DURATION_TOLERANCE_MS)
            )
        }
        else -> 0.0
    }.coerceIn(0.0, 1.0)
}

private fun isDurationAllowed(
    query: LyricQuery,
    candidate: LyricCandidate,
    titleScore: Double,
    artistScore: Double,
    durationToleranceMs: Long
): Boolean {
    if (query.durationMs <= 0L || candidate.durationMs <= 0L) return true
    val strongMetadata = titleScore >= 0.86 && artistScore >= 0.72
    val allowedTolerance = if (strongMetadata) {
        maxOf(durationToleranceMs, LYRIC_MATCH_STRONG_METADATA_DURATION_TOLERANCE_MS)
    } else {
        durationToleranceMs
    }
    return abs(query.durationMs - candidate.durationMs) <= allowedTolerance
}

private fun providerPriority(source: LyricSource): Int = when (source) {
    LyricSource.NETEASE -> 0
    LyricSource.QQ_MUSIC -> 1
    LyricSource.KUGOU -> 2
    LyricSource.BILIBILI -> 3
    LyricSource.MANUAL -> 4
}

private fun normalizeLyricMatchText(
    value: String,
    removeBracketedPrefix: Boolean = false
): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase()
    val withoutPrefix = if (removeBracketedPrefix) {
        normalized.replace(Regex("^(?:【[^】]*】|\\[[^]]*])+"), "")
    } else {
        normalized
    }
    return withoutPrefix.replace(Regex("[^\\p{L}\\p{N}]"), "")
}

private fun stringSimilarity(left: String, right: String): Double {
    if (left == right) return 1.0
    if (left.isEmpty() || right.isEmpty()) return 0.0
    return maxOf(
        lcsSimilarity(left, right),
        bigramDiceSimilarity(left, right)
    )
}

/** 最长公共子序列占比：容忍插入语/后缀差异。 */
private fun lcsSimilarity(left: String, right: String): Double {
    val previous = IntArray(right.length + 1)
    val current = IntArray(right.length + 1)
    left.forEach { leftChar ->
        for (index in right.indices) {
            current[index + 1] = if (leftChar == right[index]) {
                previous[index] + 1
            } else {
                maxOf(previous[index + 1], current[index])
            }
        }
        current.copyInto(previous)
        current.fill(0)
    }
    return previous[right.length].toDouble() / maxOf(left.length, right.length).toDouble()
}

/** 字符二元组 Dice 系数：对「长标题 vs 短歌名」的长度惩罚更宽容。 */
private fun bigramDiceSimilarity(left: String, right: String): Double {
    if (left.length < 2 || right.length < 2) {
        return if (left == right) 1.0 else 0.0
    }
    fun bigrams(value: String): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (index in 0 until value.length - 1) {
            val bigram = value.substring(index, index + 2)
            counts[bigram] = (counts[bigram] ?: 0) + 1
        }
        return counts
    }
    val leftCounts = bigrams(left)
    val rightCounts = bigrams(right)
    var overlap = 0
    leftCounts.forEach { (bigram, count) ->
        overlap += minOf(count, rightCounts[bigram] ?: 0)
    }
    val total = (left.length - 1) + (right.length - 1)
    return if (total == 0) 0.0 else 2.0 * overlap / total
}
