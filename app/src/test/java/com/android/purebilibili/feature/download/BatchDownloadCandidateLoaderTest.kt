package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BatchDownloadCandidateLoaderTest {
    private fun collection(vararg episodes: UgcEpisode) = ViewInfo(
        bvid = "current", cid = 1,
        ugc_season = UgcSeason(sections = listOf(UgcSection(episodes = episodes.toList()))),
    )

    @Test
    fun existingPagesAreReused_andDuplicateBvidLoadsOnce() = runTest {
        val requests = mutableListOf<String>()
        val known = UgcEpisode(bvid = "known", cid = 1, pages = listOf(Page(cid = 1)))
        val missing = UgcEpisode(bvid = "missing", cid = 2)
        val result = loadBatchDownloadCandidates(collection(known, missing, missing)) { bvid ->
            requests += bvid
            Result.success(ViewInfo(bvid = bvid, pages = listOf(Page(cid = 2), Page(cid = 3))))
        }
        assertEquals(listOf("missing"), requests)
        assertEquals(listOf("known#1", "missing#2", "missing#3"), result.candidates.map { it.id })
        assertTrue(result.failedBvids.isEmpty())
    }

    @Test
    fun failureKeepsKnownEntry_andRetryReusesSuccessfulDetails() = runTest {
        val info = collection(UgcEpisode(bvid = "A", cid = 1), UgcEpisode(bvid = "B", cid = 2))
        val first = loadBatchDownloadCandidates(info) { bvid ->
            if (bvid == "B") Result.failure(IllegalStateException("offline"))
            else Result.success(ViewInfo(bvid = bvid, pages = listOf(Page(cid = 1))))
        }
        assertEquals(setOf("B"), first.failedBvids)
        assertEquals(listOf("A#1", "B#2"), first.candidates.map { it.id })
        val requests = mutableListOf<String>()
        val retried = loadBatchDownloadCandidates(info, first.detailsByBvid) { bvid ->
            requests += bvid
            Result.success(ViewInfo(bvid = bvid, pages = listOf(Page(cid = 2), Page(cid = 3))))
        }
        assertEquals(listOf("B"), requests)
        assertEquals(listOf("A#1", "B#2", "B#3"), retried.candidates.map { it.id })
        assertTrue(retried.failedBvids.isEmpty())
    }

    @Test
    fun detailRequestsHaveBoundedConcurrency() = runTest {
        val episodes = (1..8).map { UgcEpisode(bvid = "BV$it", cid = it.toLong()) }
        var active = 0
        var maximum = 0
        val result = loadBatchDownloadCandidates(collection(*episodes.toTypedArray())) { bvid ->
            active++
            maximum = maxOf(maximum, active)
            try {
                delay(10)
                Result.success(ViewInfo(bvid = bvid, pages = listOf(Page(cid = bvid.removePrefix("BV").toLong()))))
            } finally { active-- }
        }
        assertEquals(4, maximum)
        assertEquals(8, result.candidates.size)
    }

    @Test
    fun cancellationIsNotConvertedToPartialSuccess() = runTest {
        assertFailsWith<CancellationException> {
            loadBatchDownloadCandidates(collection(UgcEpisode(bvid = "A", cid = 1))) {
                Result.failure(CancellationException("dismissed"))
            }
        }
    }
}
