package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheClearUiPolicyTest {

    @Test
    fun markAnimationCompleteOnlyWhenClearSucceeded() {
        assertTrue(shouldMarkCacheClearAnimationComplete(clearSucceeded = true))
        assertFalse(shouldMarkCacheClearAnimationComplete(clearSucceeded = false))
    }

    @Test
    fun resolveFailureMessageWithFallback() {
        assertEquals(
            "清理缓存失败，请稍后重试",
            resolveCacheClearFailureMessage(null)
        )
        assertEquals(
            "磁盘被占用",
            resolveCacheClearFailureMessage(IllegalStateException("磁盘被占用"))
        )
    }

    @Test
    fun defaultCacheClearTargets_focusOnPlaybackRecovery() {
        assertEquals(
            setOf(
                CacheClearTarget.PLAYBACK_QUALITY,
                CacheClearTarget.NETWORK,
                CacheClearTarget.SUBTITLE_DANMAKU
            ),
            resolveDefaultCacheClearTargets()
        )
    }

    @Test
    fun cacheClearOptions_explainSelectableCleanupScope() {
        val options = resolveCacheClearOptions()

        assertTrue(options.any { it.target == CacheClearTarget.PLAYBACK_QUALITY && it.description.contains("画质") })
        assertTrue(options.any { it.target == CacheClearTarget.IMAGE_PREVIEW && it.description.contains("预览图") })
        assertTrue(options.any { it.target == CacheClearTarget.APP_METADATA && it.description.contains("WBI") })
    }

    @Test
    fun resolveCacheClearConfirmationMessage_summarizesSelectedTargets() {
        assertEquals(
            "将清理：播放地址与画质协商缓存、网络缓存。不会删除离线缓存、下载内容和播放记录。",
            resolveCacheClearConfirmationMessage(
                setOf(
                    CacheClearTarget.PLAYBACK_QUALITY,
                    CacheClearTarget.NETWORK
                )
            )
        )
    }

    @Test
    fun resolveSelectedCacheBytes_sumsOnlyCheckedBuckets() {
        val breakdown = CacheUtils.CacheBreakdown(
            imageDiskCache = 3L * 1024 * 1024,
            imageMemoryCache = 512L * 1024,
            httpCache = 4L * 1024 * 1024,
            otherCache = 7L * 1024 * 1024,
            playUrlMemoryCache = 256L * 1024,
            subtitleDanmakuMemoryCache = 768L * 1024
        )

        assertEquals(
            (4L * 1024 * 1024) + (256L * 1024),
            resolveSelectedCacheBytes(
                breakdown = breakdown,
                selectedTargets = setOf(
                    CacheClearTarget.PLAYBACK_QUALITY,
                    CacheClearTarget.NETWORK
                )
            )
        )
    }

    @Test
    fun resolveSelectedCacheSizeSummary_updatesWithSelectedTargets() {
        val breakdown = CacheUtils.CacheBreakdown(
            imageDiskCache = 2L * 1024 * 1024,
            imageMemoryCache = 0L,
            httpCache = 1536L * 1024,
            otherCache = 5L * 1024 * 1024,
            playUrlMemoryCache = 512L * 1024,
            subtitleDanmakuMemoryCache = 0L
        )

        assertEquals(
            "已选缓存：2.0 MB",
            resolveSelectedCacheSizeSummary(
                breakdown = breakdown,
                selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW)
            )
        )
        assertEquals(
            "已选缓存：6.5 MB",
            resolveSelectedCacheSizeSummary(
                breakdown = breakdown,
                selectedTargets = setOf(
                    CacheClearTarget.NETWORK,
                    CacheClearTarget.TEMP_FILES_AND_LOGS
                )
            )
        )
    }
}
