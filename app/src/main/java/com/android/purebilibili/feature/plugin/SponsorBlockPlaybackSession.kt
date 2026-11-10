package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.SponsorBlockSessionApi
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.repository.SponsorBlockRepository
import com.android.purebilibili.plugin.sdk.SkipAction

private const val TAG = "SponsorBlockPlaybackSession"

/** A single playback's configuration snapshot, segment cursor, and skip history. */
class SponsorBlockPlaybackSession : SponsorBlockSessionApi {
    private var segments: List<SponsorSegment> = emptyList()
    private var progressMarkers: List<SponsorProgressMarker> = emptyList()
    private var nextSegmentIndex: Int = 0
    private var activeSegment: SponsorSegment? = null
    private val skippedIds = mutableSetOf<String>()
    private var config = SponsorBlockConfig().normalized()
    override suspend fun onVideoLoad(bvid: String, cid: Long) {
        // 重置状态
        segments = emptyList()
        progressMarkers = emptyList()
        skippedIds.clear()
        nextSegmentIndex = 0
        activeSegment = null
        lastPositionMs = 0
        lastAutoSkipTime = 0

        //  [修复] 加载配置
        loadConfigSuspend()

        // Failures must reach the owning ViewModel so it can retry this video.
        val loadedSegments = SponsorBlockRepository.loadSegments(
            bvid = bvid,
            cid = cid,
            categories = config.requestedCategories,
            baseUrl = config.serverBaseUrl
        )
        segments = normalizeSponsorSegments(loadedSegments).filter { segment ->
            config.behaviorFor(segment.category) != SponsorBlockSegmentBehavior.DISABLED &&
                (segment.actionType == com.android.purebilibili.data.model.response.SponsorActionType.FULL ||
                    segment.duration >= config.minimumSegmentDurationSeconds)
        }
        progressMarkers = resolveSponsorProgressMarkers(segments, config.markerMode, config.categoryColorHex)
        Logger.d(
            TAG,
            "Loaded ${segments.size} SponsorBlock segments for $bvid/$cid, autoSkip=${config.autoSkip}"
        )
    }

    // 记录上次播放位置，用于检测回拉
    private var lastPositionMs: Long = 0
    // 记录上次自动跳过的时间，用于防止跳过后的瞬间回拉误判
    private var lastAutoSkipTime: Long = 0

    override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
        if (segments.isEmpty()) return SkipAction.None

        // [修复] 检测用户回拉进度条
        // 增加防抖逻辑：如果是自动跳过后的 3 秒内，不进行回拉检测，且不更新 lastPositionMs（防止被异常值污染）
        val isGracePeriod = System.currentTimeMillis() - lastAutoSkipTime < 3000

        if (!isGracePeriod) {
            if (positionMs < lastPositionMs - 2000) {  // 回拉超过2秒
                val nextSkippedIds =
                    resetSkippedSegmentsForSeek(
                        segments = segments,
                        skippedIds = skippedIds.toSet(),
                        seekPositionMs = positionMs
                    )
                skippedIds.clear()
                skippedIds.addAll(nextSkippedIds)
                nextSegmentIndex = findCandidateSegmentIndex(positionMs)
            }
            // 只有在非 Grace Period 才更新 lastPositionMs
            // 这样如果出现跳过后的瞬间 0ms/149ms 异常值，会被忽略，保留上次的高位值
            lastPositionMs = positionMs
        } else {
            // Grace Period 内，如果 positionMs 这是正常的推移（比 lastPositionMs 大），也可以更新
            // 但如果变小了（疑似 glitch），则保持 lastPositionMs 不变
            if (positionMs > lastPositionMs) {
                lastPositionMs = positionMs
            }
        }

        while (nextSegmentIndex < segments.size) {
            val candidate = segments[nextSegmentIndex]
            if (candidate.UUID in skippedIds || positionMs > candidate.endTimeMs) {
                nextSegmentIndex += 1
                continue
            }
            break
        }

        val segment = segments.getOrNull(nextSegmentIndex)
            ?.takeIf { candidate -> positionMs in candidate.startTimeMs..candidate.endTimeMs }
            ?: run {
                activeSegment = null
                return SkipAction.None
        }
        activeSegment = segment
        val behavior = config.behaviorFor(segment.category)
        if (behavior == SponsorBlockSegmentBehavior.DISABLED) {
            skippedIds.add(segment.UUID)
            nextSegmentIndex += 1
            activeSegment = null
            return SkipAction.None
        }

        when {
            segment.isMuteType -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.Mute(
                    untilMs = segment.endTimeMs,
                    reason = "已静音: ${segment.categoryName}",
                    segmentId = segment.UUID,
                    showToast = config.skipToastEnabled,
                )
            }
            segment.isMarkerOnlyType -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.None
            }
            else -> when (behavior) {
            SponsorBlockSegmentBehavior.AUTOMATIC -> {
            skippedIds.add(segment.UUID)
            lastAutoSkipTime = System.currentTimeMillis() // 记录跳过时间
            nextSegmentIndex += 1
            activeSegment = null
            //  记录空降助手跳过事件
            com.android.purebilibili.core.util.AnalyticsHelper.logSponsorBlockSkip(
                videoId = segment.UUID,
                segmentType = segment.categoryName
            )
            return SkipAction.SkipTo(
                positionMs = segment.endTimeMs,
                reason = "已跳过: ${segment.categoryName}",
                segmentId = segment.UUID,
                startMs = segment.startTimeMs,
                category = segment.category,
                categoryName = segment.categoryName,
                showToast = config.skipToastEnabled,
            )
            }
            SponsorBlockSegmentBehavior.SKIP_ONCE -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.SkipTo(
                    positionMs = segment.endTimeMs,
                    reason = "本次跳过: ${segment.categoryName}",
                    segmentId = segment.UUID,
                    startMs = segment.startTimeMs,
                    category = segment.category,
                    categoryName = segment.categoryName,
                    showToast = config.skipToastEnabled,
                )
            }
            SponsorBlockSegmentBehavior.MANUAL -> {
                Logger.d(TAG, "🔘 显示跳过按钮: ${segment.categoryName}")
                return SkipAction.ShowButton(
                    skipToMs = segment.endTimeMs,
                    label = "跳过${segment.categoryName}",
                    segmentId = segment.UUID
                )
            }
            SponsorBlockSegmentBehavior.MARKER_ONLY,
            SponsorBlockSegmentBehavior.DISABLED -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.None
            }
            }
        }
    }

    override fun onUserSeek(positionMs: Long) {
        val nextSkippedIds =
            resetSkippedSegmentsForSeek(
                segments = segments,
                skippedIds = skippedIds.toSet(),
                seekPositionMs = positionMs
            )
        skippedIds.clear()
        skippedIds.addAll(nextSkippedIds)
        nextSegmentIndex = findCandidateSegmentIndex(positionMs)
        activeSegment = segments.getOrNull(nextSegmentIndex)
            ?.takeIf { segment -> positionMs in segment.startTimeMs..segment.endTimeMs }
        lastPositionMs = positionMs
        if (positionMs >= 0L) {
            lastAutoSkipTime = 0L
        }
    }

    /** 手动跳过时调用，标记片段已跳过 */
    override fun markAsSkipped(segmentId: String): SponsorSegment? {
        skippedIds.add(segmentId)
        val segment = segments.firstOrNull { it.UUID == segmentId }
        Logger.d(TAG, " 手动跳过完成: $segmentId")
        return segment
    }

    override fun getProgressMarkers(): List<SponsorProgressMarker> = progressMarkers
    override fun getSegments(): List<SponsorSegment> = segments
    override fun getActiveSegment(): SponsorSegment? = activeSegment
    override fun isCommunityContributionEnabled(): Boolean = config.communityContributionEnabled
    override fun getCommunityServerBaseUrl(): String = config.serverBaseUrl

    /** Sends an optional, explicitly consented community view ping for a skipped segment. */
    override suspend fun uploadViewedSegmentIfEnabled(segmentId: String) {
        if (!shouldUploadSponsorBlockView(config) || segmentId.isBlank()) return
        SponsorBlockRepository.uploadViewedSegment(
            baseUrl = config.serverBaseUrl,
            segmentId = segmentId
        ).onFailure { error ->
            Logger.w(TAG, "空降助手社区跳过记录上传失败: ${error.message}")
        }
    }

    /**
     * Submits a user-confirmed segment to the configured BilibiliSponsorBlock-compatible server.
     * Callers must collect the time range from an explicit playback UI action.
     */
    override suspend fun submitCommunitySegment(
        bvid: String,
        cid: Long,
        videoDurationSeconds: Float,
        startMs: Long,
        endMs: Long,
        category: String,
        actionType: String
    ): Result<List<SponsorSegment>> {
        if (!config.communityContributionEnabled) {
            return Result.failure(IllegalStateException("请先在空降助手设置中允许提交社区片段"))
        }
        val wholeVideo = actionType == com.android.purebilibili.data.model.response.SponsorActionType.FULL &&
            startMs == 0L && endMs == 0L
        if (bvid.isBlank() || startMs < 0L || (!wholeVideo && endMs <= startMs)) {
            return Result.failure(IllegalArgumentException("片段时间范围无效"))
        }
        if (category !in com.android.purebilibili.data.model.response.SponsorCategory.ALL_CATEGORIES) {
            return Result.failure(IllegalArgumentException("不支持的片段类别"))
        }
        if (actionType !in sponsorBlockAllowedActionTypes(category)) {
            return Result.failure(IllegalArgumentException("该类别不支持所选动作"))
        }
        return SponsorBlockRepository.submitSegments(
            baseUrl = config.serverBaseUrl,
            bvid = bvid,
            cid = cid,
            userId = config.userId,
            videoDurationSeconds = videoDurationSeconds,
            segments = listOf(
                SponsorBlockRepository.SegmentSubmission(
                    segment = listOf(startMs / 1000f, endMs / 1000f),
                    category = category,
                    actionType = actionType,
                )
            )
        )
    }

    override suspend fun voteOnCommunitySegment(segmentId: String, voteType: Int): Result<Unit> {
        if (segmentId.isBlank()) return Result.failure(IllegalArgumentException("片段标识无效"))
        return SponsorBlockRepository.voteOnSegment(config.serverBaseUrl, config.userId, segmentId, voteType = voteType)
    }

    private fun findCandidateSegmentIndex(positionMs: Long): Int {
        val index = segments.indexOfFirst { segment -> positionMs <= segment.endTimeMs }
        return if (index >= 0) index else segments.size
    }

    override fun onVideoEnd() {
        segments = emptyList()
        progressMarkers = emptyList()
        skippedIds.clear()
        lastPositionMs = 0
        nextSegmentIndex = 0
        activeSegment = null
    }

    /**  suspend版本的配置加载 */
    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            config = loadSponsorBlockConfig(context, config)
            Logger.d(TAG, "Loaded SponsorBlock config: autoSkip=${config.autoSkip}, markerMode=${config.markerMode}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load config", e)
            // Keep the existing ID and settings if reading or saving failed.
            config = config.normalized()
        }
    }
}
