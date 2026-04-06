package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryDeletePolicyTest {

    @Test
    fun `resolveHistoryDeleteKid returns archive kid for normal videos`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 540580868L, bvid = "BV1NK41157EF"),
            business = HistoryBusiness.ARCHIVE
        )

        assertEquals("archive_540580868", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns pgc kid from season id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.PGC,
            seasonId = 12345L
        )

        assertEquals("pgc_12345", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns live kid from room id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.LIVE,
            roomId = 778899L
        )

        assertEquals("live_778899", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns article kid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 334455L),
            business = HistoryBusiness.ARTICLE
        )

        assertEquals("article_334455", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid falls back to archive for unknown video-like entries`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 7788L, bvid = "BV1xx"),
            business = HistoryBusiness.UNKNOWN
        )

        assertEquals("archive_7788", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns null when unknown entry lacks valid id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.UNKNOWN
        )

        assertNull(resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryRenderKey prefers bvid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 1L, bvid = "BV1abc"),
            business = HistoryBusiness.ARCHIVE
        )

        assertEquals("BV1abc", resolveHistoryRenderKey(item))
    }

    @Test
    fun `resolveHistoryRenderKey falls back to business and oid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 2468L, bvid = ""),
            business = HistoryBusiness.PGC
        )

        assertEquals("pgc_2468", resolveHistoryRenderKey(item))
    }

    @Test
    fun `single delete should use single dissolve mode`() {
        assertEquals(
            HistoryDeleteAnimationMode.SINGLE_DISSOLVE,
            resolveHistoryDeleteAnimationMode(itemCount = 1)
        )
    }

    @Test
    fun `batch delete should skip dissolve animation and delete directly`() {
        assertEquals(
            HistoryDeleteAnimationMode.DIRECT_DELETE,
            resolveHistoryDeleteAnimationMode(itemCount = 2)
        )
        assertEquals(
            HistoryDeleteAnimationMode.DIRECT_DELETE,
            resolveHistoryDeleteAnimationMode(itemCount = 30)
        )
    }

    @Test
    fun `direct batch delete should disable jiggle and collapse`() {
        assertEquals(false, shouldJiggleHistoryDeleteCards(HistoryDeleteAnimationMode.DIRECT_DELETE))
        assertEquals(false, shouldCollapseHistoryDeleteCard(HistoryDeleteAnimationMode.DIRECT_DELETE))
    }

    @Test
    fun `single dissolve should keep jiggle and collapse`() {
        assertEquals(true, shouldJiggleHistoryDeleteCards(HistoryDeleteAnimationMode.SINGLE_DISSOLVE))
        assertEquals(true, shouldCollapseHistoryDeleteCard(HistoryDeleteAnimationMode.SINGLE_DISSOLVE))
    }

    @Test
    fun `batch delete should only finalize after every dissolving card completes`() {
        assertEquals(
            false,
            shouldFinalizeBatchHistoryDelete(
                targetKeys = setOf("a", "b", "c"),
                completedKeys = setOf("a", "b")
            )
        )
        assertEquals(
            true,
            shouldFinalizeBatchHistoryDelete(
                targetKeys = setOf("a", "b", "c"),
                completedKeys = setOf("a", "b", "c")
            )
        )
    }

    @Test
    fun `create delete session normalizes keys and chooses direct delete mode`() {
        val session = createHistoryDeleteSession(setOf(" a ", "b", ""))

        assertEquals(
            HistoryDeleteSession(
                targetKeys = setOf("a", "b"),
                completedKeys = emptySet(),
                animationMode = HistoryDeleteAnimationMode.DIRECT_DELETE
            ),
            session
        )
    }

    @Test
    fun `reduce delete session tracks completions until finalize`() {
        val session = HistoryDeleteSession(
            targetKeys = setOf("a", "b"),
            completedKeys = emptySet(),
            animationMode = HistoryDeleteAnimationMode.DIRECT_DELETE
        )

        val afterFirst = reduceHistoryDeleteSessionOnAnimationComplete(session, "a")
        val afterSecond = reduceHistoryDeleteSessionOnAnimationComplete(afterFirst, "b")

        assertEquals(setOf("a"), afterFirst.completedKeys)
        assertEquals(false, shouldFinalizeHistoryDeleteSession(afterFirst))
        assertEquals(setOf("a", "b"), afterSecond.completedKeys)
        assertEquals(true, shouldFinalizeHistoryDeleteSession(afterSecond))
    }

    @Test
    fun `active delete keys exclude completed items`() {
        val session = HistoryDeleteSession(
            targetKeys = setOf("a", "b", "c"),
            completedKeys = setOf("b"),
            animationMode = HistoryDeleteAnimationMode.DIRECT_DELETE
        )

        assertEquals(setOf("a", "c"), resolveActiveHistoryDeleteKeys(session))
    }

    @Test
    fun `completed batch item should stay hidden until session ends`() {
        val session = HistoryDeleteSession(
            targetKeys = setOf("a", "b"),
            completedKeys = setOf("a"),
            animationMode = HistoryDeleteAnimationMode.DIRECT_DELETE
        )

        assertEquals(true, shouldKeepHistoryDeletePlaceholderHidden(session, "a"))
        assertEquals(false, shouldKeepHistoryDeletePlaceholderHidden(session, "b"))
    }
}
