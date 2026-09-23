package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils
import java.io.File
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
            playbackMediaCache = 6L * 1024 * 1024,
            otherCache = 7L * 1024 * 1024,
            playUrlMemoryCache = 256L * 1024,
            subtitleDanmakuMemoryCache = 768L * 1024
        )

        assertEquals(
            (4L * 1024 * 1024) + (6L * 1024 * 1024) + (256L * 1024),
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
            playbackMediaCache = 2L * 1024 * 1024,
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
            "已选缓存：8.5 MB",
            resolveSelectedCacheSizeSummary(
                breakdown = breakdown,
                selectedTargets = setOf(
                    CacheClearTarget.NETWORK,
                    CacheClearTarget.TEMP_FILES_AND_LOGS
                )
            )
        )
    }

    @Test
    fun cacheClearAnimationDialog_drawsBehindSystemBars() {
        val source = loadCacheClearAnimationSource()

        assertTrue(
            source.contains("decorFitsSystemWindows = false"),
            "Cache clear animation dialog should be edge-to-edge so the scrim covers status and navigation bars"
        )
    }

    @Test
    fun cacheClearAnimationDialog_usesNativeCircularProgressInsteadOfCustomParticles() {
        val source = loadCacheClearAnimationSource()

        assertTrue(source.contains("AppCircularProgressIndicator"))
        assertTrue(source.contains("AdaptiveLoadingIndicator"))
        assertFalse(source.contains("DataDissolveParticles"))
        assertFalse(source.contains("CenterCleaningIcon"))
        assertFalse(source.contains("CircularProgressRing"))
    }

    @Test
    fun donutSegments_fillRingFromSelectedBytesAndUpdatePercents() {
        val breakdown = CacheUtils.CacheBreakdown(
            imageDiskCache = 50L,
            httpCache = 30L,
            otherCache = 20L
        )
        val segments = resolveCacheClearDonutSegments(
            breakdown = breakdown,
            selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW, CacheClearTarget.NETWORK)
        )
        val image = segments.first { it.target == CacheClearTarget.IMAGE_PREVIEW }
        val network = segments.first { it.target == CacheClearTarget.NETWORK }
        val temp = segments.first { it.target == CacheClearTarget.TEMP_FILES_AND_LOGS }

        assertEquals(-90f, network.startAngle)
        assertEquals(135f, network.sweepAngle, absoluteTolerance = 0.01f)
        assertEquals(225f, image.sweepAngle, absoluteTolerance = 0.01f)
        assertEquals(0f, temp.sweepAngle)
        assertEquals("62%", image.percentLabel)
        assertEquals("37%", network.percentLabel)
        assertEquals("0%", temp.percentLabel)
        assertTrue(image.selected)
        assertFalse(segments.first { it.target == CacheClearTarget.PLAYBACK_QUALITY }.selected)
    }

    @Test
    fun donutPercents_redistributeWhenASliceIsUnchecked() {
        val breakdown = CacheUtils.CacheBreakdown(
            imageDiskCache = 50L,
            httpCache = 50L
        )
        val afterUncheck = resolveCacheClearDonutSegments(
            breakdown = breakdown,
            selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW)
        )
        val image = afterUncheck.first { it.target == CacheClearTarget.IMAGE_PREVIEW }
        val network = afterUncheck.first { it.target == CacheClearTarget.NETWORK }

        assertEquals(360f, image.sweepAngle, absoluteTolerance = 0.01f)
        assertEquals("100%", image.percentLabel)
        assertEquals(0f, network.sweepAngle)
        assertEquals("0%", network.percentLabel)
        assertEquals(
            formatCacheClearBytes(50L),
            resolveCacheClearDonutCenterSize(
                breakdown = breakdown,
                selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW)
            )
        )
    }

    @Test
    fun donutHitTarget_ignoresHoleAndSelectsClockwiseSlice() {
        val segments = resolveCacheClearDonutSegments(
            breakdown = CacheUtils.CacheBreakdown(
                imageDiskCache = 50L,
                httpCache = 50L
            ),
            selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW, CacheClearTarget.NETWORK)
        )

        assertEquals(
            CacheClearTarget.NETWORK,
            resolveCacheClearDonutHitTarget(
                segments = segments,
                dx = 0f,
                dy = -40f,
                innerRadius = 20f,
                outerRadius = 50f
            )
        )
        assertEquals(
            CacheClearTarget.IMAGE_PREVIEW,
            resolveCacheClearDonutHitTarget(
                segments = segments,
                dx = 0f,
                dy = 40f,
                innerRadius = 20f,
                outerRadius = 50f
            )
        )
        assertEquals(
            null,
            resolveCacheClearDonutHitTarget(
                segments = segments,
                dx = 0f,
                dy = -8f,
                innerRadius = 20f,
                outerRadius = 50f
            )
        )
    }

    @Test
    fun clearButtonLabel_usesSelectedBytes() {
        val breakdown = CacheUtils.CacheBreakdown(imageDiskCache = 2L * 1024 * 1024)
        assertEquals(
            "清理缓存 2.0 MB",
            resolveCacheClearButtonLabel(
                breakdown = breakdown,
                selectedTargets = setOf(CacheClearTarget.IMAGE_PREVIEW)
            )
        )
        assertEquals(
            "清理缓存",
            resolveCacheClearButtonLabel(
                breakdown = breakdown,
                selectedTargets = emptySet()
            )
        )
    }

    private fun loadCacheClearAnimationSource(): String {
        val candidates = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/CacheClearAnimation.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/CacheClearAnimation.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate CacheClearAnimation.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
