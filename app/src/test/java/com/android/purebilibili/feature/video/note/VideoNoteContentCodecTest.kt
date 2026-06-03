package com.android.purebilibili.feature.video.note

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoNoteContentCodecTest {

    @Test
    fun encodeKeepsRichTextAttributesAndTimestampTag() {
        val document = VideoNoteEditorDocument(
            title = "标题",
            blocks = listOf(
                VideoNoteBlock.Text("重点", bold = true, highlight = true),
                VideoNoteBlock.Text("列表项", unorderedList = true),
                VideoNoteBlock.Timestamp(seconds = 72, cid = 100L, index = 0, cidCount = 2)
            )
        )

        val encoded = VideoNoteContentCodec.encode(document)

        assertTrue(encoded.content.contains("\"bold\":true"))
        assertTrue(encoded.content.contains("\"background\":\"#fff359\""))
        assertTrue(encoded.content.contains("\"list\":\"bullet\""))
        assertTrue(encoded.content.contains("\"seconds\":72"))
        assertTrue(encoded.tags.contains("\"pos\":2"))
        assertEquals("重点列表项[01:12]", encoded.summary)
    }

    @Test
    fun decodeRestoresKnownBlocksAndIgnoresInvalidPayload() {
        val content = """
            [
              {"attributes":{"bold":true},"insert":"标题\n"},
              {"insert":{"tag":{"cid":100,"status":0,"index":1,"seconds":45,"cidCount":3,"title":"00:45"}}}
            ]
        """.trimIndent()

        val decoded = VideoNoteContentCodec.decode(title = "笔记", content = content)

        assertEquals("笔记", decoded.title)
        assertEquals(VideoNoteBlock.Text("标题\n", bold = true), decoded.blocks[0])
        assertEquals(
            VideoNoteBlock.Timestamp(seconds = 45, cid = 100L, index = 1, cidCount = 3, label = "00:45"),
            decoded.blocks[1]
        )
        assertEquals(emptyList(), VideoNoteContentCodec.decode(title = "", content = "bad json").blocks)
    }
}
