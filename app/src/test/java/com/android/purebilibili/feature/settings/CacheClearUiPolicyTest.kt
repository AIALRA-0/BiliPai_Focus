package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        assertEquals("50%", image.percentLabel)
        assertEquals("30%", network.percentLabel)
        assertEquals("20%", temp.percentLabel)
        assertEquals("63%", image.chartPercentLabel)
        assertEquals("37%", network.chartPercentLabel)
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
        assertEquals("50%", image.percentLabel)
        assertEquals("100%", image.chartPercentLabel)
        assertEquals(0f, network.sweepAngle)
        assertEquals("50%", network.percentLabel)
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

    @Test
    fun unknownAndZeroCachesDoNotCreateInventedSlices() {
        val selected = CacheClearTarget.entries.toSet()
        val unknown = resolveCacheClearDonutSegments(null, selected)
        assertTrue(unknown.all { it.sweepAngle == 0f && it.percentLabel == "—" && !it.sizeKnown })
        val empty = resolveCacheClearDonutSegments(CacheUtils.CacheBreakdown(), selected)
        assertTrue(empty.all { it.sweepAngle == 0f && it.percentLabel == "0%" })
        assertFalse(empty.first { it.target == CacheClearTarget.APP_METADATA }.sizeKnown)
        assertEquals("0%", formatCacheClearPercent(0L, 100L))
    }

    @Test
    fun roundedPercentsPreserveTotalAndExcludeZeroBuckets() {
        assertEquals(listOf(34, 33, 33, 0), roundedCacheClearPercents(listOf(1L, 1L, 1L, 0L)))
        assertEquals(listOf(0, 0), roundedCacheClearPercents(listOf(0L, 0L)))
        assertEquals(100, roundedCacheClearPercents(listOf(99999999999L, 1L, 40000L)).sum())
    }

    @Test
    fun tinyCategoriesRemainTouchableWithoutInventingZeroShares() {
        val fractions = cacheClearVisualFractions(listOf(999L, 1L, 0L))
        assertEquals(.98f, fractions[0], .0001f)
        assertEquals(.02f, fractions[1], .0001f)
        assertEquals(0f, fractions[2])
        assertEquals(1f, fractions.sum(), .0001f)
    }

    @Test
    fun metadataOnlySelectionRemainsActionableWithoutDiskShare() {
        val targets = setOf(CacheClearTarget.APP_METADATA)
        val segments = resolveCacheClearDonutSegments(CacheUtils.CacheBreakdown(httpCache = 500L), targets)
        assertTrue(segments.all { it.sweepAngle == 0f })
        assertTrue(segments.first { it.target == CacheClearTarget.APP_METADATA }.selected)
        assertEquals("清理缓存 0 KB", resolveCacheClearButtonLabel(CacheUtils.CacheBreakdown(), targets))
    }

    @Test
    fun progressDistinguishesPreparationFromRealCompletedSteps() {
        assertNull(cacheClearProgressFraction(0, 0))
        assertEquals(.5f, cacheClearProgressFraction(2, 4))
        assertEquals(0f, cacheClearProgressFraction(-1, 4))
        assertEquals(1f, cacheClearProgressFraction(9, 4))
    }
}
