package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoCommentLoadPolicyTest {

    @Test
    fun `comment skeleton slot count fills the viewport within sane bounds`() {
        assertEquals(VIDEO_COMMENT_SKELETON_MIN_SLOTS, resolveVideoCommentSkeletonSlotCount(0))
        assertEquals(VIDEO_COMMENT_SKELETON_MIN_SLOTS, resolveVideoCommentSkeletonSlotCount(200))
        assertEquals(5, resolveVideoCommentSkeletonSlotCount(600))
        assertEquals(VIDEO_COMMENT_SKELETON_MAX_SLOTS, resolveVideoCommentSkeletonSlotCount(4_000))
    }

    @Test
    fun `comment page size is at least the legacy thirty so first screen needs fewer pages`() {
        // 骨架与真实内容的高度差、触底加载次数都随每页条数上升而下降，不再回退到 20。
        assertTrue(VIDEO_COMMENT_PAGE_SIZE >= 30)
    }

    @Test
    fun `comment cache key separates subject type sort mode and viewer`() {
        val base = videoCommentCacheKey(oid = 1L, type = 1, sortModeApiMode = 3, viewerMid = 10L)
        assertEquals(
            base,
            videoCommentCacheKey(oid = 1L, type = 1, sortModeApiMode = 3, viewerMid = 10L),
        )
        assertTrue(base != videoCommentCacheKey(oid = 2L, type = 1, sortModeApiMode = 3, viewerMid = 10L))
        assertTrue(base != videoCommentCacheKey(oid = 1L, type = 12, sortModeApiMode = 3, viewerMid = 10L))
        assertTrue(base != videoCommentCacheKey(oid = 1L, type = 1, sortModeApiMode = 2, viewerMid = 10L))
        // 点赞/点踩态随账号变化，所以观看者 mid 必须进 key，避免换账号读到上一个账号的态。
        assertTrue(base != videoCommentCacheKey(oid = 1L, type = 1, sortModeApiMode = 3, viewerMid = 11L))
    }

    @Test
    fun `comment cache serves the last page and evicts least recently used`() {
        val cache = VideoCommentCache(maxEntries = 2)
        cache.put("a", snapshot(rpid = 1L))
        cache.put("b", snapshot(rpid = 2L))

        // 读取 a 使其成为最近使用，随后写入 c 时最久未用的 b 应被淘汰。
        assertEquals(listOf(1L), cache.get("a")?.replies?.map { it.rpid })
        cache.put("c", snapshot(rpid = 3L))

        assertEquals(2, cache.size())
        assertNull(cache.get("b"))
        assertEquals(listOf(1L), cache.get("a")?.replies?.map { it.rpid })
        assertEquals(listOf(3L), cache.get("c")?.replies?.map { it.rpid })
    }

    @Test
    fun `comment cache ignores empty payloads and blank keys`() {
        val cache = VideoCommentCache()
        cache.put("a", snapshot(rpid = 1L))
        cache.put("b", snapshot(rpid = 2L).copy(replies = emptyList()))
        cache.put("", snapshot(rpid = 3L))

        assertEquals(listOf(1L), cache.get("a")?.replies?.map { it.rpid })
        // 空列表与空 key 都不进缓存，避免「重进秒出一个空骨架」。
        assertNull(cache.get("b"))
        assertNull(cache.get(""))
    }

    private fun snapshot(rpid: Long) = VideoCommentCacheSnapshot(
        replies = listOf(ReplyItem(rpid = rpid)),
        replyCount = 1,
        nextPage = 2,
        isEnd = false,
        grpcNextOffset = null,
        upMid = 0L,
        pinnedReplyIds = emptySet(),
        subject = CommentSubjectKey(oid = 1L),
        cachedAtMs = 0L,
    )
}
