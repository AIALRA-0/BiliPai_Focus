package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.ViewInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.coroutineContext

internal data class BatchDownloadCandidateLoadResult(
    val candidates: List<BatchDownloadCandidate>,
    val detailsByBvid: Map<String, ViewInfo> = emptyMap(),
    val failedBvids: Set<String> = emptySet(),
)

/** 仅补取缺少分 P 的稿件详情，最多同时请求四个；调用者关闭弹窗时取消。 */
internal suspend fun loadBatchDownloadCandidates(
    info: ViewInfo,
    knownDetails: Map<String, ViewInfo> = emptyMap(),
    loadDetail: suspend (String) -> Result<ViewInfo>,
): BatchDownloadCandidateLoadResult = coroutineScope {
    val episodes = info.ugc_season?.sections.orEmpty().flatMap { it.episodes }
    val missingBvids = episodes.groupBy { it.bvid.trim() }
        .filter { (bvid, entries) ->
            bvid.isNotBlank() &&
                knownDetails[bvid]?.pages.orEmpty().none { it.cid > 0L } &&
                entries.flatMap { it.pages }.none { it.cid > 0L } &&
                !(bvid == info.bvid && info.pages.any { it.cid > 0L })
        }.keys
    val limiter = Semaphore(4)
    val loaded = missingBvids.map { bvid ->
        async {
            limiter.withPermit {
                val detail = try {
                    loadDetail(bvid).getOrThrow().takeIf { loaded ->
                        loaded.bvid == bvid && loaded.pages.any { it.cid > 0L }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                bvid to detail
            }
        }
    }.awaitAll()
    coroutineContext.ensureActive()
    val details = knownDetails + loaded.mapNotNull { (bvid, detail) -> detail?.let { bvid to it } }.toMap()
    BatchDownloadCandidateLoadResult(
        candidates = resolveBatchDownloadCandidates(info, details),
        detailsByBvid = details,
        failedBvids = loaded.filter { it.second == null }.map { it.first }.toSet(),
    )
}
