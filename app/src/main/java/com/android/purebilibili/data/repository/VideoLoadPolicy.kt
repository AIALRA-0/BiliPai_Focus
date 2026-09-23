package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.Page

internal enum class PlayUrlSource {
    APP,
    DASH,
    HTML5,
    LEGACY,
    GUEST
}

internal enum class PlayUrlRequestKind {
    INITIAL,
    PLAYBACK_TRANSITION,
    EXPLICIT
}

internal fun isStrictPremiumQualityRequest(
    requestKind: PlayUrlRequestKind,
    targetQn: Int
): Boolean {
    return requestKind == PlayUrlRequestKind.EXPLICIT && (targetQn == 100 || targetQn >= 112)
}

internal data class VideoInfoLookupInput(
    val bvid: String,
    val aid: Long
)

internal fun resolveVideoInfoLookupInput(rawBvid: String, aid: Long): VideoInfoLookupInput? {
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

internal fun resolveRequestedVideoCid(
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

internal fun resolveInitialStartQuality(
    targetQuality: Int?,
    isAutoHighestQuality: Boolean,
    isLogin: Boolean,
    isVip: Boolean,
    auto1080pEnabled: Boolean
): Int {
    return when {
        // The API documents 127 as the highest qn and 126/125 as separate Dolby/HDR
        // tiers. Requesting 125 here can omit the Dolby track until a manual switch.
        isAutoHighestQuality && isVip -> 127
        isAutoHighestQuality && isLogin -> 80
        isAutoHighestQuality -> 64
        targetQuality != null -> targetQuality
        isVip -> 116
        isLogin && auto1080pEnabled -> 80
        isLogin -> 64
        else -> 32
    }
}

internal fun resolveVideoPlaybackAuthState(
    hasSessionCookie: Boolean,
    hasAccessToken: Boolean
): Boolean {
    return hasSessionCookie
}

internal fun shouldSkipPlayUrlCache(
    isAutoHighestQuality: Boolean,
    isVip: Boolean,
    audioLang: String?
): Boolean {
    // Language-specific streams must not reuse the default-language cache entry.
    // VIP auto-highest used to always skip cache and hurt re-open TTFF; accept cache
    // when [shouldAcceptCachedPlayUrlForAutoHighest] says the payload is good enough.
    return audioLang != null
}

/**
 * VIP auto-highest can reuse a cached playurl when it already contains a premium track.
 * Thin/low-quality cache entries are ignored so we still re-negotiate high quality.
 */
internal fun shouldAcceptCachedPlayUrlForAutoHighest(
    isAutoHighestQuality: Boolean,
    isVip: Boolean,
    cachedDashVideoIds: List<Int>,
    minPremiumQuality: Int = 112
): Boolean {
    if (!isAutoHighestQuality || !isVip) return true
    return cachedDashVideoIds.any { it >= minPremiumQuality }
}

/**
 * Build the DASH playurl attempt chain for a target quality.
 *
 * The requested quality itself must lead the list. Premium tiers such as 8K (127),
 * Dolby Vision (126), and HDR (125) are not always bundled into a lower-qn response
 * (e.g. qn=120), so omitting the target causes QUALITY_SWITCH_FAILURE / no HDR tracks.
 */
internal fun buildDashAttemptQualities(targetQn: Int): List<Int> {
    if (targetQn <= 80) return listOf(targetQn)

    val premiumQualities = listOf(129, 127, 126, 125, 120, 116, 112, 100)
    val lowerFallbacks = premiumQualities.filter { quality -> quality < targetQn }

    return (listOf(targetQn) + lowerFallbacks + 80).distinct()
}

internal fun resolveDashRetryDelays(
    targetQn: Int,
    isPrimaryAttempt: Boolean = false
): List<Long> {
    // 播放接口偶发以 code=0 返回空流。首次目标画质应先原档重试，避免自动最高模式
    // 立即逐档降级，最终只保留 Legacy/Guest 返回的 720P、360P 轨道。
    // 后续高级画质 fallback 不重复重试，防止短时间内放大请求并触发接口风控。
    return if (isPrimaryAttempt || targetQn <= 80) listOf(0L, 450L) else listOf(0L)
}

internal fun shouldRetryOnlyTransientEmptyDashResponse(
    targetQn: Int,
    isPrimaryAttempt: Boolean,
): Boolean = isPrimaryAttempt && targetQn > 80

internal fun shouldRetryDashTrackRecovery(
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

internal fun buildStartQualityDecisionSummary(
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

internal fun buildPlayUrlFetchSummary(
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

internal fun shouldCallAccessTokenApi(
    nowMs: Long,
    cooldownUntilMs: Long,
    hasAccessToken: Boolean
): Boolean {
    return hasAccessToken && nowMs >= cooldownUntilMs
}

internal fun shouldTryAppApiForTargetQuality(
    targetQn: Int,
    hasSessionCookie: Boolean = true,
    directedTrafficMode: Boolean = false
): Boolean {
    // BiliPai parity: playback stays on the Web/WBI playurl path instead of
    // prioritizing the APP access_token endpoint for 1080P and premium tiers.
    return false
}

internal fun shouldEnableDirectedTrafficMode(
    directedTrafficEnabled: Boolean,
    isOnMobileData: Boolean
): Boolean {
    return directedTrafficEnabled && isOnMobileData
}

internal fun buildDirectedTrafficWbiOverrides(
    directedTrafficEnabled: Boolean,
    isOnMobileData: Boolean
): Map<String, String> {
    if (!shouldEnableDirectedTrafficMode(directedTrafficEnabled, isOnMobileData)) {
        return emptyMap()
    }
    return mapOf(
        "platform" to "android",
        "mobi_app" to "android",
        "device" to "android",
        "build" to "8130300"
    )
}

internal fun buildPlayUrlWbiBaseParams(
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
        "web_location" to "1315873"
    )
    if (tryLook) {
        params["try_look"] = "1"
    }
    if (!audioLang.isNullOrEmpty()) {
        params["cur_language"] = audioLang
    }
    return params
}

internal fun shouldRequestPlayUrlTryLook(
    isLoggedIn: Boolean,
    auto1080pEnabled: Boolean
): Boolean {
    return !isLoggedIn && auto1080pEnabled
}

internal fun buildLoggedInPlaybackFallbackOrder(): List<PlayUrlSource> {
    return listOf(
        PlayUrlSource.DASH,
        PlayUrlSource.APP,
        PlayUrlSource.LEGACY,
        PlayUrlSource.GUEST
    )
}

internal fun buildGuestPlaybackFallbackOrder(): List<PlayUrlSource> {
    return listOf(
        PlayUrlSource.DASH,
        PlayUrlSource.LEGACY
    )
}

internal fun shouldAcceptAppApiResultForTargetQuality(
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
    // finish an explicit high-quality request; otherwise continue to APP fallback.
    return targetQn in dashVideoIds
}

internal fun buildGuestFallbackQualities(): List<Int> {
    return listOf(80, 64, 32)
}

internal fun shouldCachePlayUrlResult(
    source: PlayUrlSource,
    audioLang: String?,
    requestedQuality: Int = 0,
    returnedQuality: Int = 0,
    dashVideoIds: List<Int> = emptyList()
): Boolean {
    if (audioLang != null) return false
    if (source == PlayUrlSource.GUEST) return false
    if (requestedQuality >= 80 && !isRequestedQualitySatisfied(
            requestedQuality = requestedQuality,
            returnedQuality = returnedQuality,
            dashVideoIds = dashVideoIds
        )
    ) {
        return false
    }
    return true
}

internal fun isRequestedQualitySatisfied(
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
internal fun hasExactPlayableRequestedTrack(
    requestedTargetQn: Int,
    dashVideos: List<DashVideo>
): Boolean {
    return dashVideos.any { video ->
        video.id == requestedTargetQn && video.getValidUrl().isNotEmpty()
    }
}

internal fun isExactRequestedQualitySelected(
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
 * 6. Current DASH does not already include HDR track 125
 * 7. User hasn't made an explicit quality selection
 * 8. Upgrade hasn't already been attempted for this playback session
 */
internal fun shouldScheduleHdrAutoUpgrade(
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
        125 !in currentPlayableDashVideoIds &&
        !userHasExplicitQualitySelection &&
        !upgradeAlreadyAttempted
}

internal fun shouldFetchCommentEmoteMapOnVideoLoad(): Boolean {
    return false
}

internal fun shouldRefreshVipStatusOnVideoLoad(): Boolean {
    return false
}

internal fun shouldFetchInteractionStatusOnVideoLoad(): Boolean {
    return false
}
