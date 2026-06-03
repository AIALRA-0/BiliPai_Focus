package com.android.purebilibili.feature.video.note

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoNoteTagPolicyTest {

    @Test
    fun encodeTagsUsesTimestampBlockPosition() {
        val tags = VideoNoteTagPolicy.encodeTags(
            listOf(
                VideoNoteBlock.Text("开头"),
                VideoNoteBlock.Timestamp(seconds = 12, cid = 100L, index = 0, cidCount = 1),
                VideoNoteBlock.Text("正文"),
                VideoNoteBlock.Timestamp(seconds = 88, cid = 101L, index = 1, cidCount = 2)
            )
        )

        assertTrue(tags.contains("\"seconds\":12"))
        assertTrue(tags.contains("\"pos\":1"))
        assertTrue(tags.contains("\"seconds\":88"))
        assertTrue(tags.contains("\"pos\":3"))
    }

    @Test
    fun encodeTagsReturnsEmptyArrayWhenNoTimestampExists() {
        assertEquals("[]", VideoNoteTagPolicy.encodeTags(listOf(VideoNoteBlock.Text("正文"))))
    }
}
