package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumeNoticePolicyTest {
    @Test
    fun `history restoration is announced but restarts and handoffs are silent`() {
        assertTrue(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 0L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(0L, 0L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 436_000L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, true, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, false, true))
    }

    @Test
    fun `notice waits for ready player at the restored position`() {
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 0L, true))
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 436_000L, false))
        assertTrue(hasReachedSavedPlaybackPosition(436_000L, 436_000L, true))
        assertTrue(hasReachedSavedPlaybackPosition(436_000L, 437_000L, true))
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 600_000L, true))
    }

    @Test
    fun `position message supports minutes and hour long videos`() {
        assertEquals("欢迎回来，已为您定位至 07:16", buildPlaybackResumeNoticeMessage(436_000L))
        assertEquals("欢迎回来，已为您定位至 01:07:16", buildPlaybackResumeNoticeMessage(4_036_000L))
    }
    @Test
    fun serverPartHistoryMapsToTheCurrentVideoAndTargetPart() {
        val info = ViewInfo(
            bvid = "BVmulti", cid = 101L,
            pages = listOf(Page(cid = 101L, page = 1), Page(cid = 202L, page = 2)),
        )
        val notice = assertNotNull(resolvePartResumeNotice(7L, info, 202L, 0L, true))
        assertEquals(7L, notice.requestToken)
        assertEquals("BVmulti", notice.bvid)
        assertEquals(101L, notice.currentCid)
        assertEquals(202L, notice.targetCid)
        assertEquals(2, notice.partNumber)
        assertNull(resolvePartResumeNotice(7L, info, 202L, 0L, false))
        assertNull(resolvePartResumeNotice(7L, info, 101L, 0L, true))
        assertNull(resolvePartResumeNotice(7L, info, 0L, 0L, true))
        assertNull(resolvePartResumeNotice(7L, info, 999L, 0L, true))
    }

}
