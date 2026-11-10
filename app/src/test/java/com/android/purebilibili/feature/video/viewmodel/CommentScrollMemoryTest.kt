package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CommentScrollMemoryTest {
    @Test
    fun reopeningSameVideoAndSortRestoresPosition() {
        val memory = CommentScrollMemory()
        val session = memory.open(123, CommentSortMode.HOT)
        memory.save(session, 12, 85)
        val restored = memory.open(123, CommentSortMode.HOT)
        assertSame(session, restored)
        assertEquals(12, restored.index)
        assertEquals(85, restored.offset)
    }

    @Test
    fun changingSortOrVideoStartsAtTopAndRejectsOldWrites() {
        val memory = CommentScrollMemory()
        val hot = memory.open(123, CommentSortMode.HOT)
        memory.save(hot, 12, 85)
        memory.clear()
        val newest = memory.open(123, CommentSortMode.NEWEST)
        memory.save(hot, 99, 999)
        assertEquals(0, newest.index)
        assertEquals(0, newest.offset)
        memory.save(newest, 3, 20)
        val otherVideo = memory.open(456, CommentSortMode.NEWEST)
        assertEquals(0, otherVideo.index)
        assertEquals(0, otherVideo.offset)
    }
}
