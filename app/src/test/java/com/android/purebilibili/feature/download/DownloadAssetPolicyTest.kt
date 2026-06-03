package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadAssetPolicyTest {

    @Test
    fun parallelChunksRequireKnownLargeRangeDownload() {
        assertTrue(shouldUseParallelChunks(totalBytes = 9L * 1024L * 1024L, supportsRanges = true))
        assertFalse(shouldUseParallelChunks(totalBytes = 7L * 1024L * 1024L, supportsRanges = true))
        assertFalse(shouldUseParallelChunks(totalBytes = 9L * 1024L * 1024L, supportsRanges = false))
    }

    @Test
    fun chunkRangesCoverWholeFileWithoutOverlap() {
        val ranges = resolveDownloadChunkRanges(totalBytes = 10L, maxChunks = 4)

        assertEquals(
            listOf(
                DownloadChunkRange(index = 0, startBytes = 0L, endBytesInclusive = 2L),
                DownloadChunkRange(index = 1, startBytes = 3L, endBytesInclusive = 5L),
                DownloadChunkRange(index = 2, startBytes = 6L, endBytesInclusive = 7L),
                DownloadChunkRange(index = 3, startBytes = 8L, endBytesInclusive = 9L)
            ),
            ranges
        )
        assertEquals(10L, ranges.sumOf { it.sizeBytes })
    }

    @Test
    fun contentRangeProbeParsesTotalBytes() {
        val probe = parseDownloadContentRange("bytes 0-0/12345")

        assertEquals(DownloadRangeProbe(totalBytes = 12345L, supportsRanges = true), probe)
    }

    @Test
    fun completedAssetReuseRequiresExactKnownSize() {
        assertTrue(shouldReuseCompletedAsset(outputBytes = 100L, totalBytes = 100L))
        assertFalse(shouldReuseCompletedAsset(outputBytes = 120L, totalBytes = 100L))
        assertFalse(shouldReuseCompletedAsset(outputBytes = 100L, totalBytes = 0L))
    }

    @Test
    fun oversizedCompletedAssetIsDiscarded() {
        assertTrue(shouldDiscardCompletedAsset(outputBytes = 120L, totalBytes = 100L))
        assertFalse(shouldDiscardCompletedAsset(outputBytes = 100L, totalBytes = 100L))
        assertFalse(shouldDiscardCompletedAsset(outputBytes = 120L, totalBytes = 0L))
    }

    @Test
    fun chunkCompletionRejectsOversizedOrShortChunks() {
        assertEquals(80L, resolveCompletedChunkBytes(chunkBytes = 80L, expectedBytes = 100L))
        assertEquals(100L, resolveCompletedChunkBytes(chunkBytes = 100L, expectedBytes = 100L))
        assertEquals(0L, resolveCompletedChunkBytes(chunkBytes = 120L, expectedBytes = 100L))
        assertFalse(isDownloadChunkComplete(chunkBytes = 80L, expectedBytes = 100L))
        assertTrue(isDownloadChunkComplete(chunkBytes = 100L, expectedBytes = 100L))
    }

    @Test
    fun assetStateReplacesSameKindOnly() {
        val task = baseTask
            .withAssetState(DownloadAssetState(kind = DownloadAssetKind.VIDEO, status = DownloadAssetStatus.PENDING))
            .withAssetState(DownloadAssetState(kind = DownloadAssetKind.AUDIO, status = DownloadAssetStatus.COMPLETED))
            .withAssetState(DownloadAssetState(kind = DownloadAssetKind.VIDEO, status = DownloadAssetStatus.FAILED))

        assertEquals(2, task.assets.size)
        assertEquals(DownloadAssetStatus.FAILED, task.assetState(DownloadAssetKind.VIDEO)?.status)
        assertEquals(DownloadAssetStatus.COMPLETED, task.assetState(DownloadAssetKind.AUDIO)?.status)
    }

    private val baseTask = DownloadTask(
        bvid = "BV1asset",
        cid = 1L,
        title = "缓存视频",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "",
        duration = 120,
        quality = 80,
        qualityDesc = "1080P",
        videoUrl = "video",
        audioUrl = "audio"
    )
}
