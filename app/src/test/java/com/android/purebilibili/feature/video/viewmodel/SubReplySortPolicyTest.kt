package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubReplySortPolicyTest {
    @Test
    fun `sort modes match PiliPlus DetailList protocol`() {
        assertEquals(2, SubReplySortMode.TIME.apiMode)
        assertEquals(3, SubReplySortMode.HOT.apiMode)
        assertEquals(SubReplySortMode.TIME, SubReplyUiState().sortMode)
        assertEquals(SubReplySortMode.HOT, SubReplySortMode.TIME.toggled())
        assertEquals(SubReplySortMode.TIME, SubReplySortMode.HOT.toggled())
    }

    @Test
    fun `changing sort discards old pages cursor and deep link target`() {
        val root = ReplyItem(rpid = 10L)
        val reply = ReplyItem(rpid = 11L)
        val current = SubReplyUiState(
            visible = true,
            rootReply = root,
            items = persistentListOf(reply),
            baseItems = persistentListOf(reply),
            page = 4,
            basePage = 4,
            totalCount = 200,
            isEnd = true,
            baseIsEnd = true,
            grpcNextOffset = "old-time-cursor",
            baseGrpcNextOffset = "old-time-cursor",
            targetReplyId = 11L,
            error = "old error",
        )
        for (mode in SubReplySortMode.entries) {
            val reset = current.resetForSort(mode)
            assertEquals(mode, reset.sortMode)
            assertEquals(root, reset.rootReply)
            assertEquals(200, reset.totalCount)
            assertTrue(reset.visible)
            assertTrue(reset.isLoading)
            assertTrue(reset.items.isEmpty())
            assertTrue(reset.baseItems.isEmpty())
            assertEquals(1, reset.page)
            assertEquals(1, reset.basePage)
            assertFalse(reset.isEnd)
            assertFalse(reset.baseIsEnd)
            assertNull(reset.grpcNextOffset)
            assertNull(reset.baseGrpcNextOffset)
            assertNull(reset.error)
            assertEquals(0L, reset.targetReplyId)
        }
    }

    @Test
    fun `sorted pagination follows cursor instead of declared reply count`() {
        assertFalse(isSortedSubReplyPageEnd(false, "next-page"))
        assertTrue(isSortedSubReplyPageEnd(true, "next-page"))
        assertTrue(isSortedSubReplyPageEnd(false, null))
        assertTrue(isSortedSubReplyPageEnd(false, ""))
    }
}
