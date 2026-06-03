package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.Page
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoLoadPolicyTest {

    @Test
    fun `resolveVideoInfoLookup prefers bv id`() {
        val input = resolveVideoInfoLookupInput(rawBvid = " BV1xx411c7mD ", aid = 0L)

        assertEquals(VideoInfoLookupInput(bvid = "BV1xx411c7mD", aid = 0L), input)
    }

    @Test
    fun `resolveVideoInfoLookup parses av id when aid missing`() {
        val input = resolveVideoInfoLookupInput(rawBvid = "av1129813966", aid = 0L)

        assertEquals(VideoInfoLookupInput(bvid = "", aid = 1129813966L), input)
    }

    @Test
    fun `resolveVideoInfoLookup falls back to explicit aid`() {
        val input = resolveVideoInfoLookupInput(rawBvid = "", aid = 1756441068L)

        assertEquals(VideoInfoLookupInput(bvid = "", aid = 1756441068L), input)
    }

    @Test
    fun `resolveInitialStartQuality uses stable quality for non vip auto highest`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 127,
            isAutoHighestQuality = true,
            isLogin = true,
            isVip = false,
            auto1080pEnabled = true
        )

        assertEquals(80, quality)
    }

    @Test
    fun `resolveInitialStartQuality keeps stable first request for vip auto highest`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 127,
            isAutoHighestQuality = true,
            isLogin = true,
            isVip = true,
            auto1080pEnabled = true
        )

        assertEquals(120, quality)
    }

    @Test
    fun `resolveInitialStartQuality respects explicit low preference`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 32,
            isAutoHighestQuality = false,
            isLogin = false,
            isVip = false,
            auto1080pEnabled = false
        )

        assertEquals(32, quality)
    }

    @Test
    fun `resolveInitialStartQuality respects explicit premium preference for vip`() {
        val quality = resolveInitialStartQuality(
            targetQuality = 120,
            isAutoHighestQuality = false,
            isLogin = true,
            isVip = true,
            auto1080pEnabled = true
        )

        assertEquals(120, quality)
    }

    @Test
    fun `shouldSkipPlayUrlCache only skips auto highest when vip`() {
        assertFalse(
            shouldSkipPlayUrlCache(
                isAutoHighestQuality = true,
                isVip = false,
                audioLang = null
            )
        )
        assertTrue(
            shouldSkipPlayUrlCache(
                isAutoHighestQuality = true,
                isVip = true,
                audioLang = null
            )
        )
    }

    @Test
    fun `buildDashAttemptQualities includes premium fallbacks for high target`() {
        assertEquals(listOf(120, 116, 112, 80), buildDashAttemptQualities(120))
        assertEquals(listOf(80), buildDashAttemptQualities(80))
    }

    @Test
    fun `buildDashAttemptQualities walks premium 1080p tiers before plain 1080p`() {
        assertEquals(listOf(120, 116, 112, 80), buildDashAttemptQualities(120))
        assertEquals(listOf(116, 112, 80), buildDashAttemptQualities(116))
        assertEquals(listOf(112, 80), buildDashAttemptQualities(112))
    }

    @Test
    fun `resolveDashRetryDelays allows one retry for standard qualities`() {
        assertEquals(listOf(0L), resolveDashRetryDelays(120))
        assertEquals(listOf(0L, 450L), resolveDashRetryDelays(80))
        assertEquals(listOf(0L, 450L), resolveDashRetryDelays(64))
    }

    @Test
    fun `shouldRetryDashTrackRecovery retries when api advertises 1080p but first dash payload is capped at 720p`() {
        assertTrue(
            shouldRetryDashTrackRecovery(
                targetQn = 80,
                returnedQuality = 64,
                acceptQualities = listOf(80, 64, 32),
                dashVideoIds = listOf(64, 32)
            )
        )
    }

    @Test
    fun `shouldRetryDashTrackRecovery skips retry when target quality track already exists`() {
        assertFalse(
            shouldRetryDashTrackRecovery(
                targetQn = 80,
                returnedQuality = 80,
                acceptQualities = listOf(80, 64, 32),
                dashVideoIds = listOf(80, 64, 32)
            )
        )
    }

    @Test
    fun `shouldRetryDashTrackRecovery skips retry when api never advertised target quality`() {
        assertFalse(
            shouldRetryDashTrackRecovery(
                targetQn = 80,
                returnedQuality = 64,
                acceptQualities = listOf(64, 32),
                dashVideoIds = listOf(64, 32)
            )
        )
    }

    @Test
    fun `buildStartQualityDecisionSummary includes auth and quality context`() {
        assertEquals(
            "PLAY_DIAG start_quality bvid=BV1TEST12345 cid=9527 userSetting=116 start=116 autoHighest=false isLoggedIn=true isVip=false auto1080p=true audioLang=default",
            buildStartQualityDecisionSummary(
                bvid = "BV1TEST12345",
                cid = 9527L,
                userSettingQuality = 116,
                startQuality = 116,
                isAutoHighestQuality = false,
                isLoggedIn = true,
                isVip = false,
                auto1080pEnabled = true,
                audioLang = null
            )
        )
    }

    @Test
    fun `buildPlayUrlFetchSummary includes source result and advertised tracks`() {
        assertEquals(
            "PLAY_DIAG fetch_result bvid=BV1TEST12345 cid=9527 source=DASH requested=80 returned=64 accept=[80, 64, 32] dash=[64, 32] hasDurl=false isLoggedIn=true isVip=false audioLang=default",
            buildPlayUrlFetchSummary(
                bvid = "BV1TEST12345",
                cid = 9527L,
                source = PlayUrlSource.DASH,
                requestedQuality = 80,
                returnedQuality = 64,
                acceptQualities = listOf(80, 64, 32),
                dashVideoIds = listOf(64, 32),
                hasDurl = false,
                isLoggedIn = true,
                isVip = false,
                audioLang = null
            )
        )
    }

    @Test
    fun `shouldCallAccessTokenApi respects cooldown`() {
        val now = 1_000L
        assertFalse(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 2_000L, hasAccessToken = true))
        assertTrue(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 500L, hasAccessToken = true))
        assertFalse(shouldCallAccessTokenApi(nowMs = now, cooldownUntilMs = 500L, hasAccessToken = false))
    }

    @Test
    fun `shouldTryAppApiForTargetQuality stays disabled for PiliPlus parity playback strategy`() {
        assertFalse(shouldTryAppApiForTargetQuality(targetQn = 80, hasSessionCookie = false))
        assertFalse(shouldTryAppApiForTargetQuality(targetQn = 80, hasSessionCookie = true))
        assertFalse(shouldTryAppApiForTargetQuality(64))
        assertFalse(shouldTryAppApiForTargetQuality(112))
        assertFalse(shouldTryAppApiForTargetQuality(120))
        assertFalse(
            shouldTryAppApiForTargetQuality(
                targetQn = 64,
                hasSessionCookie = true,
                directedTrafficMode = true
            )
        )
    }

    @Test
    fun `shouldAcceptAppApiResultForTargetQuality keeps downgraded playable 1080 response for startup recovery`() {
        assertTrue(
            shouldAcceptAppApiResultForTargetQuality(
                requestKind = PlayUrlRequestKind.INITIAL,
                targetQn = 80,
                returnedQuality = 64,
                dashVideoIds = listOf(64, 32)
            )
        )
    }

    @Test
    fun `shouldCachePlayUrlResult rejects downgraded high quality startup result`() {
        assertFalse(
            shouldCachePlayUrlResult(
                source = PlayUrlSource.DASH,
                audioLang = null,
                requestedQuality = 80,
                returnedQuality = 32,
                dashVideoIds = listOf(32)
            )
        )
        assertTrue(
            shouldCachePlayUrlResult(
                source = PlayUrlSource.DASH,
                audioLang = null,
                requestedQuality = 80,
                returnedQuality = 64,
                dashVideoIds = listOf(80, 64, 32)
            )
        )
    }

    @Test
    fun `shouldEnableDirectedTrafficMode only when enabled on mobile network`() {
        assertTrue(
            shouldEnableDirectedTrafficMode(
                directedTrafficEnabled = true,
                isOnMobileData = true
            )
        )
        assertFalse(
            shouldEnableDirectedTrafficMode(
                directedTrafficEnabled = true,
                isOnMobileData = false
            )
        )
        assertFalse(
            shouldEnableDirectedTrafficMode(
                directedTrafficEnabled = false,
                isOnMobileData = true
            )
        )
    }

    @Test
    fun `buildDirectedTrafficWbiOverrides returns android app params in directed traffic mode`() {
        val overrides = buildDirectedTrafficWbiOverrides(
            directedTrafficEnabled = true,
            isOnMobileData = true
        )
        assertEquals("android", overrides["platform"])
        assertEquals("android", overrides["mobi_app"])
        assertEquals("android", overrides["device"])
        assertEquals("8130300", overrides["build"])

        assertTrue(
            buildDirectedTrafficWbiOverrides(
                directedTrafficEnabled = false,
                isOnMobileData = true
            ).isEmpty()
        )
    }

    @Test
    fun `buildPlayUrlWbiBaseParams omits try look by default for logged in parity`() {
        val params = buildPlayUrlWbiBaseParams(
            bvid = "BV1TEST12345",
            cid = 9527L,
            qn = 80,
            audioLang = "ja"
        )

        assertEquals("BV1TEST12345", params["bvid"])
        assertEquals("9527", params["cid"])
        assertEquals("80", params["qn"])
        assertEquals("4048", params["fnval"])
        assertEquals("1", params["fourk"])
        assertEquals("1", params["voice_balance"])
        assertEquals("pre-load", params["gaia_source"])
        assertEquals("true", params["isGaiaAvoided"])
        assertEquals("1315873", params["web_location"])
        assertEquals("ja", params["cur_language"])
        assertFalse(params.containsKey("try_look"))
        assertFalse(params.containsKey("session"))
        assertFalse(params.containsKey("high_quality"))
        assertFalse(params.containsKey("platform"))
    }

    @Test
    fun `buildPlayUrlWbiBaseParams includes try look only when explicitly requested`() {
        val params = buildPlayUrlWbiBaseParams(
            bvid = "BV1TEST12345",
            cid = 9527L,
            qn = 80,
            tryLook = true
        )

        assertEquals("1", params["try_look"])
    }

    @Test
    fun `shouldRequestPlayUrlTryLook only enables guest preview 1080 mode`() {
        assertFalse(
            shouldRequestPlayUrlTryLook(
                isLoggedIn = true,
                auto1080pEnabled = true
            )
        )
        assertTrue(
            shouldRequestPlayUrlTryLook(
                isLoggedIn = false,
                auto1080pEnabled = true
            )
        )
        assertFalse(
            shouldRequestPlayUrlTryLook(
                isLoggedIn = false,
                auto1080pEnabled = false
            )
        )
    }

    @Test
    fun `buildLoggedInPlaybackFallbackOrder keeps WBI main path but preserves auth recovery chain`() {
        assertEquals(
            listOf(PlayUrlSource.DASH, PlayUrlSource.APP, PlayUrlSource.LEGACY, PlayUrlSource.GUEST),
            buildLoggedInPlaybackFallbackOrder()
        )
    }

    @Test
    fun `buildGuestPlaybackFallbackOrder keeps legacy fallback when WBI returns empty payload`() {
        assertEquals(
            listOf(PlayUrlSource.DASH, PlayUrlSource.LEGACY),
            buildGuestPlaybackFallbackOrder()
        )
    }

    @Test
    fun `shouldAcceptAppApiResultForTargetQuality rejects downgraded high quality response for explicit selection`() {
        assertFalse(
            shouldAcceptAppApiResultForTargetQuality(
                requestKind = PlayUrlRequestKind.EXPLICIT,
                targetQn = 120,
                returnedQuality = 80,
                dashVideoIds = listOf(80, 64)
            )
        )
    }

    @Test
    fun `shouldAcceptAppApiResultForTargetQuality accepts when target exists in dash list`() {
        assertTrue(
            shouldAcceptAppApiResultForTargetQuality(
                requestKind = PlayUrlRequestKind.EXPLICIT,
                targetQn = 120,
                returnedQuality = 80,
                dashVideoIds = listOf(120, 80, 64)
            )
        )
    }

    @Test
    fun `resolveVideoPlaybackAuthState requires session cookie for web playurl auth`() {
        assertTrue(resolveVideoPlaybackAuthState(hasSessionCookie = true, hasAccessToken = false))
        assertFalse(resolveVideoPlaybackAuthState(hasSessionCookie = false, hasAccessToken = true))
        assertFalse(resolveVideoPlaybackAuthState(hasSessionCookie = false, hasAccessToken = false))
    }

    @Test
    fun `resolveRequestedVideoCid prefers valid request cid`() {
        val cid = resolveRequestedVideoCid(
            requestCid = 22L,
            infoCid = 11L,
            pages = listOf(Page(cid = 11L), Page(cid = 22L))
        )

        assertEquals(22L, cid)
    }

    @Test
    fun `resolveRequestedVideoCid falls back to info cid when request cid missing in pages`() {
        val cid = resolveRequestedVideoCid(
            requestCid = 33L,
            infoCid = 11L,
            pages = listOf(Page(cid = 11L), Page(cid = 22L))
        )

        assertEquals(11L, cid)
    }

    @Test
    fun `resolveRequestedVideoCid accepts request cid when pages absent`() {
        val cid = resolveRequestedVideoCid(
            requestCid = 33L,
            infoCid = 11L,
            pages = emptyList()
        )

        assertEquals(33L, cid)
    }

    @Test
    fun `buildGuestFallbackQualities prefers 80 before 64`() {
        assertEquals(listOf(80, 64, 32), buildGuestFallbackQualities())
    }

    @Test
    fun `shouldCachePlayUrlResult skips guest source`() {
        assertFalse(shouldCachePlayUrlResult(PlayUrlSource.GUEST, audioLang = null))
        assertTrue(shouldCachePlayUrlResult(PlayUrlSource.DASH, audioLang = null))
        assertFalse(shouldCachePlayUrlResult(PlayUrlSource.DASH, audioLang = "en"))
    }

    @Test
    fun `shouldFetchCommentEmoteMapOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldFetchCommentEmoteMapOnVideoLoad())
    }

    @Test
    fun `shouldRefreshVipStatusOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldRefreshVipStatusOnVideoLoad())
    }

    @Test
    fun `shouldFetchInteractionStatusOnVideoLoad keeps first frame path lean`() {
        assertFalse(shouldFetchInteractionStatusOnVideoLoad())
    }
}
