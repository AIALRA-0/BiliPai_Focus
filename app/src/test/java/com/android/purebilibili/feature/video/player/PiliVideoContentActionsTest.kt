package com.android.purebilibili.feature.video.player

import com.android.purebilibili.data.model.response.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PiliVideoContentActionsTest {
    @Test
    fun timestampUsesCurrentCidAndZeroBasedPageRatherThanFirstPage() {
        val pages = listOf(Page(cid = 101L, page = 1), Page(cid = 202L, page = 2))
        val stamp = requireNotNull(buildPiliVideoNoteTimestamp(202L, pages, 65_999L))
        assertEquals(202L, stamp.cid)
        assertEquals(1, stamp.index)
        assertEquals(2, stamp.cidCount)
        assertEquals(65L, stamp.seconds)
    }

    @Test
    fun foreignCidCannotBeAssignedToAnotherVideosPages() {
        assertNull(buildPiliVideoNoteTimestamp(303L, listOf(Page(cid = 101L)), 1000L))
    }

    @Test
    fun missingCidCannotCreateTimestamp() {
        assertNull(buildPiliVideoNoteTimestamp(0L, emptyList(), 1000L))
        assertNull(buildPiliVideoNoteTimestamp(-1L, emptyList(), 1000L))
    }

    @Test
    fun absentPagesPreserveKnownCurrentCidAndClampUnsetPlayerPosition() {
        val stamp = requireNotNull(buildPiliVideoNoteTimestamp(202L, emptyList(), Long.MIN_VALUE))
        assertEquals(202L, stamp.cid)
        assertEquals(0, stamp.index)
        assertEquals(1, stamp.cidCount)
        assertEquals(0L, stamp.seconds)
    }
}
