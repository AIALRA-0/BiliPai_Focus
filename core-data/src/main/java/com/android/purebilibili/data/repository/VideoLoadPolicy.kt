package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.Page

enum class PlayUrlSource {
    APP,
    DASH,
    HTML5,
    LEGACY,
    GUEST
}

enum class PlayUrlRequestKind {
    INITIAL,
    PLAYBACK_TRANSITION,
    EXPLICIT
}

fun isStrictPremiumQualityRequest(
    requestKind: PlayUrlRequestKind,
    targetQn: Int
): Boolean {
    return requestKind == PlayUrlRequestKind.EXPLICIT && (targetQn == 100 || targetQn >= 112)
}

data class VideoInfoLookupInput(
    val bvid: String,
    val aid: Long
)

fun resolveVideoInfoLookupInput(rawBvid: String, aid: Long): VideoInfoLookupInput? {
    val normalizedBvid = rawBvid.trim()
    if (normalizedBvid.startsWith("BV", ignoreCase = true)) {
        return VideoInfoLookupInput(bvid = normalizedBvid, aid = 0L)
    }

    if (aid > 0L) {
        return VideoInfoLookupInput(bvid = "", aid = aid)
    }

    val normalizedAv = normalizedBvid.lowercase()
    if (normalizedAv.startsWith("av")) {
        val parsedAid = normalizedAv.removePrefix("av").toLongOrNull()
        if (parsedAid != null && parsedAid > 0L) {
            return VideoInfoLookupInput(bvid = "", aid = parsedAid)
        }
    }

    normalizedBvid.toLongOrNull()?.takeIf { it > 0L }?.let { parsedAid ->
        return VideoInfoLookupInput(bvid = "", aid = parsedAid)
    }

    return null
}

fun resolveRequestedVideoCid(
    requestCid: Long,
    infoCid: Long,
    pages: List<Page>
): Long {
    val normalizedRequestCid = requestCid.takeIf { it > 0L }
    val normalizedInfoCid = infoCid.takeIf { it > 0L }

    if (normalizedRequestCid != null) {
        if (pages.isEmpty() || pages.any { it.cid == normalizedRequestCid }) {
            return normalizedRequestCid
        }
    }

    return normalizedInfoCid ?: normalizedRequestCid ?: 0L
}

/** 低清目标直接取流；其余目标先发现画质目录，再由播放层选择偏好。 */
fun resolveInitialStartQuality(targetQuality: Int? = null): Int =
    targetQuality?.takeIf { it in 1..32 } ?: 129

fun isPlayUrlRiskControlResponse(code: Int, hasVoucher: Boolean): Boolean =
    hasVoucher || code in setOf(-351, -352, -412)

fun resolveVideoPlaybackAuthState(
    hasSessionCookie: Boolean,
    hasAccessToken: Boolean
): Boolean {
    return hasSessionCookie
}

fun shouldSkipPlayUrlCache(
    isAutoHighestQuality: Boolean,
    isVip: Boolean,
    audioLang: String?
): Boolean {
    // Language-specific streams must not reuse the default-language cache entry.
    // Highest-tier discovery caches the account's returned catalogue, including
    // legitimate lower-quality results for videos without premium representations.
    return audioLang != null
}

/** 同一播放请求只查询目标画质，不按档位放大失败请求。 */
fun buildDashAttemptQualities(targetQn: Int): List<Int> = listOf(targetQn)

fun resolveDashRetryDelays(targetQn: Int, isPrimaryAttempt: Boolean = false): List<Long> = listOf(0L)

fun shouldRetryOnlyTransientEmptyDashResponse(targetQn: Int, isPrimaryAttempt: Boolean): Boolean = false

fun shouldRetryDashTrackRecovery(
    targetQn: Int,
    returnedQuality: Int,
    acceptQualities: List<Int>,
    dashVideoIds: List<Int>
): Boolean {
    if (targetQn <= 0 || targetQn > 80) return false
    if (returnedQuality >= targetQn) return false
    if (targetQn !in acceptQualities) return false
    return targetQn !in dashVideoIds
}

fun buildStartQualityDecisionSummary(
    bvid: String,
    cid: Long,
    userSettingQuality: Int?,
    startQuality: Int,
    isAutoHighestQuality: Boolean,
    isLoggedIn: Boolean,
    isVip: Boolean,
    auto1080pEnabled: Boolean,
    audioLang: String?
): String {
    return "PLAY_DIAG start_quality bvid=$bvid cid=$cid userSetting=${userSettingQuality ?: "null"} " +
        "start=$startQuality autoHighest=$isAutoHighestQuality " +
        "isLoggedIn=$isLoggedIn isVip=$isVip auto1080p=$auto1080pEnabled " +
        "audioLang=${audioLang ?: "default"}"
}

fun buildPlayUrlFetchSummary(
    bvid: String,
    cid: Long,
    source: PlayUrlSource,
    requestedQuality: Int,
    returnedQuality: Int,
    acceptQualities: List<Int>,
    dashVideoIds: List<Int>,
    hasDurl: Boolean,
    isLoggedIn: Boolean,
    isVip: Boolean,
    audioLang: String?
): String {
    return "PLAY_DIAG fetch_result bvid=$bvid cid=$cid source=$source requested=$requestedQuality " +
        "returned=$returnedQuality accept=$acceptQualities dash=$dashVideoIds " +
        "hasDurl=$hasDurl isLoggedIn=$isLoggedIn isVip=$isVip " +
        "audioLang=${audioLang ?: "default"}"
}

fun shouldCallAccessTokenApi(
    nowMs: Long,
    cooldownUntilMs: Long,
    hasAccessToken: Boolean
): Boolean {
    return hasAccessToken && nowMs >= cooldownUntilMs
}

fun shouldTryAppApiForTargetQuality(
    targetQn: Int,
    hasSessionCookie: Boolean = true,
    directedTrafficMode: Boolean = false
): Boolean {
    // BiliPai parity: playback stays on the Web/WBI playurl path instead of
    // prioritizing the APP access_token endpoint for 1080P and premium tiers.
    return false
}

fun shouldEnableDirectedTrafficMode(
    directedTrafficEnabled: Boolean,
    isOnMobileData: Boolean
): Boolean {
    return directedTrafficEnabled && isOnMobileData
}

fun buildDirectedTrafficWbiOverrides(
    directedTrafficEnabled: Boolean,
    isOnMobileData: Boolean
): Map<String, String> {
    // 定向流量设置只影响媒体地址处理，不能改变 Web 签名请求的平台身份。
    return emptyMap()
}

fun buildPlayUrlWbiBaseParams(
    bvid: String,
    cid: Long,
    qn: Int,
    audioLang: String? = null,
    tryLook: Boolean = false
): MutableMap<String, String> {
    val params = linkedMapOf(
        "bvid" to bvid,
        "cid" to cid.toString(),
        "qn" to qn.toString(),
        "fnval" to "4048",
        "fnver" to "0",
        "fourk" to "1",
        "voice_balance" to "1",
        "gaia_source" to "pre-load",
        "isGaiaAvoided" to "true",
        "web_location" to "1315873",
        "dm_img_list" to "[]",
        "dm_img_str" to randomPlayUrlFingerprint(16, 64),
        "dm_cover_img_str" to randomPlayUrlFingerprint(32, 128),
        "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}"
    )
    if (tryLook) {
        params["try_look"] = "1"
    }
    if (!audioLang.isNullOrEmpty()) {
        params["cur_language"] = audioLang
    }
    return params
}

fun shouldRequestPlayUrlTryLook(
    isLoggedIn: Boolean,
    auto1080pEnabled: Boolean
): Boolean {
    return !isLoggedIn && auto1080pEnabled
}

private fun randomPlayUrlFingerprint(minLength: Int, maxLength: Int): String {
    val bytes = ByteArray(kotlin.random.Random.nextInt(minLength, maxLength + 1)) {
        kotlin.random.Random.nextInt(0x26, 0x7f).toByte()
    }
    return java.util.Base64.getEncoder().encodeToString(bytes).dropLast(2)
}

fun buildLoggedInPlaybackFallbackOrder(): List<PlayUrlSource> = listOf(PlayUrlSource.DASH)

fun buildGuestPlaybackFallbackOrder(): List<PlayUrlSource> = listOf(PlayUrlSource.DASH)

fun shouldAcceptAppApiResultForTargetQuality(
    requestKind: PlayUrlRequestKind,
    targetQn: Int,
    returnedQuality: Int,
    dashVideoIds: List<Int>
): Boolean {
    if (requestKind != PlayUrlRequestKind.EXPLICIT) {
        // Startup and media transitions should keep any playable payload to avoid a hard
        // failure when the service downgrades or the next part lacks the previous part's tier.
        return returnedQuality > 0 || dashVideoIds.isNotEmpty()
    }

    if (isStrictPremiumQualityRequest(requestKind, targetQn)) {
        return targetQn in dashVideoIds
    }

    // Explicit quality selection must respect the requested target for both VIP
    // and non-VIP users; otherwise the UI reports a successful switch while the
    // backend silently returns a lower tier.
    if (targetQn < 80) return true
    // In DASH responses `quality` is response metadata, not proof that the
    // requested representation is playable. Only an exact playable track may
    // finish an explicit high-quality request; otherwise report the unavailable tier.
    return targetQn in dashVideoIds
}

fun buildGuestFallbackQualities(): List<Int> {
    return listOf(80, 64, 32)
}

fun shouldCachePlayUrlResult(
    source: PlayUrlSource,
    audioLang: String?,
    requestedQuality: Int = 0,
    returnedQuality: Int = 0,
    dashVideoIds: List<Int> = emptyList(),
    requestKind: PlayUrlRequestKind = PlayUrlRequestKind.EXPLICIT
): Boolean {
    if (audioLang != null) return false
    if (source == PlayUrlSource.GUEST) return false
    // A discovery request may legitimately return a lower tier for this account/video.
    if (requestKind == PlayUrlRequestKind.EXPLICIT && requestedQuality >= 80 && !isRequestedQualitySatisfied(
            requestedQuality = requestedQuality,
            returnedQuality = returnedQuality,
            dashVideoIds = dashVideoIds
        )
    ) {
        return false
    }
    return true
}

fun isRequestedQualitySatisfied(
    requestedQuality: Int,
    returnedQuality: Int,
    dashVideoIds: List<Int>
): Boolean {
    if (requestedQuality < 80) return true
    return requestedQuality in dashVideoIds
}

/**
 * Returns true when the DASH manifest contains a playable exact track the user requested.
 *
 * Premium qualities (125 HDR, 126 Dolby Vision, 127 8K) must not be considered
 * satisfied by a lower-tier response or by an exact-ID track without a URL.
 */
fun hasExactPlayableRequestedTrack(
    requestedTargetQn: Int,
    dashVideos: List<DashVideo>
): Boolean {
    return dashVideos.any { video ->
        video.id == requestedTargetQn && video.getValidUrl().isNotEmpty()
    }
}

fun isExactRequestedQualitySelected(
    requestedTargetQn: Int,
    actualQuality: Int
): Boolean = actualQuality == requestedTargetQn

/**
 * Determines whether a non-blocking HDR auto-upgrade should be scheduled after
 * an INITIAL SDR fast-start playback.
 *
 * All conditions must be met:
 * 1. This is an INITIAL (first-load) request
 * 2. User has auto-highest quality enabled
 * 3. User is VIP (premium account)
 * 4. Not on mobile data (Wi-Fi only)
 * 5. Valid access_token available
 * 6. Current DASH does not already include HDR or a higher tier (125–129)
 * 7. User hasn't made an explicit quality selection
 * 8. Upgrade hasn't already been attempted for this playback session
 */
fun shouldScheduleHdrAutoUpgrade(
    isInitialRequest: Boolean,
    isAutoHighestQuality: Boolean,
    isVip: Boolean,
    isMobileData: Boolean,
    hasAccessToken: Boolean,
    currentPlayableDashVideoIds: List<Int>,
    userHasExplicitQualitySelection: Boolean,
    upgradeAlreadyAttempted: Boolean
): Boolean {
    return isInitialRequest &&
        isAutoHighestQuality &&
        isVip &&
        !isMobileData &&
        hasAccessToken &&
        currentPlayableDashVideoIds.none { it >= 125 } &&
        !userHasExplicitQualitySelection &&
        !upgradeAlreadyAttempted
}

fun shouldFetchCommentEmoteMapOnVideoLoad(): Boolean {
    return false
}

fun shouldRefreshVipStatusOnVideoLoad(): Boolean {
    return false
}

fun shouldFetchInteractionStatusOnVideoLoad(): Boolean {
    return false
}
