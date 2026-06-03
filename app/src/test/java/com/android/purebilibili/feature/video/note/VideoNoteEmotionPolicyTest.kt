package com.android.purebilibili.feature.video.note

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoNoteEmotionPolicyTest {

    @Test
    fun emptyMessageExplainsActionableState() {
        assertEquals(
            "登录后可以把这条视频的重点留成笔记。",
            resolveVideoNoteEmptyMessage(isLoggedIn = false, forbidNoteEntrance = false)
        )
        assertEquals(
            "还没有笔记，可以边看边记下时间点。",
            resolveVideoNoteEmptyMessage(isLoggedIn = true, forbidNoteEntrance = false)
        )
        assertEquals(
            "该视频暂不支持笔记。",
            resolveVideoNoteEmptyMessage(isLoggedIn = true, forbidNoteEntrance = true)
        )
    }

    @Test
    fun saveFeedbackKeepsAiDraftContext() {
        assertEquals("笔记已保存。", resolveVideoNoteSaveFeedback(fromAiSummary = false))
        assertEquals("AI 草稿已保存为视频笔记。", resolveVideoNoteSaveFeedback(fromAiSummary = true))
    }

    @Test
    fun conflictMessageDistinguishesAppendFromCreate() {
        assertEquals(
            "这条视频已有私有笔记，AI 草稿会追加到当前笔记末尾，保存前仍可修改。",
            resolveVideoNoteConflictMessage(hasExistingPrivateNote = true)
        )
        assertEquals(
            "已根据 AI 总结生成草稿，确认后再保存到笔记。",
            resolveVideoNoteConflictMessage(hasExistingPrivateNote = false)
        )
    }
}
