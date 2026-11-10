package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.CommentMainListContinuation
import com.android.purebilibili.data.model.response.ReplyItem
import kotlin.math.ceil

/**
 * 视频评论区首页每页条数。
 *
 * 从 20 提到 30：首屏一次拿到的评论更多，既减少触底加载次数，也让首屏骨架与真实内容
 * 的高度差更小（骨架槽位按 130dp 估高，与真实一级评论行高同量级）。
 */
internal const val VIDEO_COMMENT_PAGE_SIZE = 30

/**
 * 评论骨架槽位的估算高度（dp），对齐真实一级评论行高。
 *
 * 骨架行高与真实行高越接近，「骨架 → 内容」替换时的列表位移越小。
 */
internal const val VIDEO_COMMENT_SKELETON_SLOT_HEIGHT_DP = 130f
const val VIDEO_COMMENT_SKELETON_MIN_SLOTS = 3
const val VIDEO_COMMENT_SKELETON_MAX_SLOTS = 8

/**
 * 按可视高度估算首屏需要铺多少个骨架槽位。
 *
 * 宁可多铺一点（多余槽位在屏幕外，LazyList 不会组合它们，没有成本），也不要少铺——
 * 少铺会让列表在数据到达时突然被撑长，产生位移。
 */
internal fun resolveVideoCommentSkeletonSlotCount(viewportHeightDp: Int): Int {
    if (viewportHeightDp <= 0) return VIDEO_COMMENT_SKELETON_MIN_SLOTS
    val estimated = ceil(viewportHeightDp / VIDEO_COMMENT_SKELETON_SLOT_HEIGHT_DP).toInt()
    return estimated.coerceIn(VIDEO_COMMENT_SKELETON_MIN_SLOTS, VIDEO_COMMENT_SKELETON_MAX_SLOTS)
}

/**
 * 进程内评论缓存的分桶键：同一个评论主体 + 同一种排序 + 同一个观看者。
 *
 * 带上观看者 mid 是为了避免换账号后读到上一个账号的点赞/点踩态（`action` 字段随账号变化）。
 */
internal fun videoCommentCacheKey(
    oid: Long,
    type: Int,
    sortModeApiMode: Int,
    viewerMid: Long,
): String = "$oid#$type#$sortModeApiMode#$viewerMid"

/**
 * 评论首页缓存快照。
 *
 * 只缓存「首页」：重进同一个视频的评论区时先秒出旧评论，再后台静默刷新，既避免重复
 * 转圈，也不会让缓存无限膨胀（后续页仍走网络，但首屏已经有内容兜底）。
 */
internal data class VideoCommentCacheSnapshot(
    val replies: List<ReplyItem>,
    val replyCount: Int,
    val nextPage: Int,
    val isEnd: Boolean,
    val grpcNextOffset: String?,
    val upMid: Long,
    val pinnedReplyIds: Set<Long>,
    val subject: CommentSubjectKey,
    val cachedAtMs: Long,
    val mainListContinuation: CommentMainListContinuation? = null,
    val pageControl: com.android.purebilibili.data.model.response.ReplyPageControl? = null,
)

/**
 * 进程内 LRU 评论缓存（手写 LRU，不引入依赖）。
 *
 * 生命周期与进程一致、跨 ViewModel 实例复用：退出视频再进、切走再回来都能秒出旧评论。
 * 不落盘；`get` 用「先删再插」保持最近使用语义，`put` 时按容量淘汰最久未用的桶。
 */
internal class VideoCommentCache(private val maxEntries: Int = 12) {

    private val entries = LinkedHashMap<String, VideoCommentCacheSnapshot>()

    @Synchronized
    internal fun get(key: String): VideoCommentCacheSnapshot? {
        val snapshot = entries.remove(key) ?: return null
        // 重新插入到队尾，保持「最近使用」语义
        entries[key] = snapshot
        return snapshot
    }

    @Synchronized
    internal fun put(key: String, snapshot: VideoCommentCacheSnapshot) {
        if (key.isBlank() || snapshot.replies.isEmpty()) return
        entries.remove(key)
        entries[key] = snapshot
        while (entries.size > maxEntries) {
            val oldestKey = entries.keys.firstOrNull() ?: break
            entries.remove(oldestKey)
        }
    }

    @Synchronized
    internal fun size(): Int = entries.size

    @Synchronized
    internal fun clear() {
        entries.clear()
    }
}

/**
 * 进程内共享实例。
 *
 * 刻意做成进程级而不是 ViewModel 级：视频详情页退出后 ViewModel 就没了，缓存挂在
 * ViewModel 上会跟着一起丢，而「退出再进」恰恰是最需要秒出的场景。
 * 所有读写都加了 [Synchronized]，可安全地从不同 ViewModel 的协程并发访问。
 */
internal val sharedVideoCommentCache = VideoCommentCache()
