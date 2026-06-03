package com.android.purebilibili.feature.video.note

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoNoteEditorStatePolicyTest {
    @Test
    fun aiDraftWithoutSavedNoteCanContinueEditing() {
        val draft = VideoNoteEditorDocument(
            title = "视频标题",
            blocks = listOf(VideoNoteBlock.Text("AI 总结草稿"))
        )
        val noteState = VideoNoteUiState(
            editorDocument = draft,
            editorFromAiSummary = true
        )

        assertTrue(hasUnsavedVideoNoteDraft(noteState))
        assertEquals("继续编辑", resolveVideoNotePrimaryActionLabel(noteState))
        assertEquals(draft, resolveVideoNoteEditableDocument(noteState, defaultTitle = "默认标题"))
    }

    @Test
    fun savedNotePrefersEditWhenNoUnsavedDraftExists() {
        val saved = VideoNoteEditorDocument(
            title = "已保存",
            blocks = listOf(VideoNoteBlock.Text("正文"))
        )
        val noteState = VideoNoteUiState(
            privateNoteDocument = saved,
            editorDocument = saved
        )

        assertFalse(hasUnsavedVideoNoteDraft(noteState))
        assertEquals("编辑", resolveVideoNotePrimaryActionLabel(noteState))
        assertEquals(saved, resolveVideoNoteEditableDocument(noteState, defaultTitle = "默认标题"))
    }

    @Test
    fun unsavedAiAppendOnSavedNoteCanContinueEditing() {
        val saved = VideoNoteEditorDocument(
            title = "已保存",
            blocks = listOf(VideoNoteBlock.Text("原正文"))
        )
        val draft = saved.copy(
            blocks = saved.blocks + VideoNoteBlock.Text("\nAI 补充")
        )
        val noteState = VideoNoteUiState(
            privateNoteDocument = saved,
            editorDocument = draft,
            editorFromAiSummary = true
        )

        assertTrue(hasUnsavedVideoNoteDraft(noteState))
        assertEquals("继续编辑", resolveVideoNotePrimaryActionLabel(noteState))
        assertEquals(draft, resolveVideoNoteEditableDocument(noteState, defaultTitle = "默认标题"))
    }
}
