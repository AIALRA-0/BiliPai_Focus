package com.android.purebilibili.feature.video.note

import com.android.purebilibili.data.model.response.AiModelResult
import com.android.purebilibili.data.model.response.AiOutline
import com.android.purebilibili.data.model.response.AiPartOutline
import com.android.purebilibili.data.model.response.AiSummaryData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiSummaryNoteDraftPolicyTest {

    @Test
    fun aiSummaryBuildsEditableTimestampDraft() {
        val draft = buildVideoNoteDraftFromAiSummary(
            title = "视频标题",
            aiSummary = AiSummaryData(
                code = 0,
                modelResult = AiModelResult(
                    summary = "整体摘要",
                    outline = listOf(
                        AiOutline(
                            title = "开场",
                            timestamp = 5,
                            partOutline = listOf(AiPartOutline(timestamp = 8, content = "细节点"))
                        )
                    )
                )
            ),
            cid = 123L,
            pageIndex = 0,
            cidCount = 1
        )

        assertEquals("视频标题", draft.title)
        assertTrue(draft.blocks.contains(VideoNoteBlock.Text("整体摘要\n\n")))
        assertTrue(draft.blocks.any { it is VideoNoteBlock.Timestamp && it.seconds == 5L })
        assertTrue(draft.blocks.any { it is VideoNoteBlock.Timestamp && it.seconds == 8L })
    }

    @Test
    fun aiSummaryDraftAppendsToExistingPrivateNote() {
        val existing = VideoNoteEditorDocument(
            title = "原笔记",
            blocks = listOf(VideoNoteBlock.Text("原内容"))
        )

        val draft = buildVideoNoteDraftFromAiSummary(
            title = "新标题",
            aiSummary = AiSummaryData(code = 0, modelResult = AiModelResult(summary = "AI 摘要")),
            cid = 1L,
            pageIndex = 0,
            cidCount = 1,
            existingDocument = existing
        )

        assertEquals("原笔记", draft.title)
        assertEquals(VideoNoteBlock.Text("原内容"), draft.blocks.first())
        assertTrue(VideoNoteContentCodec.toPlainText(draft).contains("尚未保存"))
        assertTrue(VideoNoteContentCodec.toPlainText(draft).contains("AI 摘要"))
    }
}
