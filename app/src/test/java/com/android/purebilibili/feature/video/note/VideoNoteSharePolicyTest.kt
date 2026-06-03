package com.android.purebilibili.feature.video.note

import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNoteSharePolicyTest {
    @Test
    fun buildShareTextIncludesNoteVideoAndTimestamp() {
        val text = buildVideoNoteShareText(
            videoTitle = "测试视频",
            bvid = "BV1xx411c7mD",
            document = VideoNoteEditorDocument(
                title = "我的笔记",
                blocks = listOf(
                    VideoNoteBlock.Text("开头重点\n"),
                    VideoNoteBlock.Timestamp(seconds = 90, cid = 1, index = 0, cidCount = 1),
                    VideoNoteBlock.Text("这里值得回看")
                )
            )
        )

        assertTrue(text.contains("我的笔记"))
        assertTrue(text.contains("[01:30]"))
        assertTrue(text.contains("来自视频：测试视频"))
        assertTrue(text.contains("https://www.bilibili.com/video/BV1xx411c7mD"))
    }

    @Test
    fun aiDraftShareTextMarksDraftWithoutSavingImplication() {
        val text = buildVideoNoteShareText(
            videoTitle = "测试视频",
            bvid = "BV1xx411c7mD",
            document = VideoNoteEditorDocument(
                blocks = listOf(VideoNoteBlock.Text("AI 总结内容"))
            ),
            isDraft = true
        )

        assertTrue(text.contains("AI 整理草稿"))
        assertTrue(text.contains("测试视频"))
        assertTrue(text.contains("AI 总结内容"))
    }
}
