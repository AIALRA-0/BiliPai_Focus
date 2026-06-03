package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommentInputDialogLayoutPolicyTest {

    @Test
    fun portrait_keepsRoomyEditorForComfortableCommentInput() {
        val policy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = false
        )

        assertEquals(84, policy.inputBoxMinHeightDp)
        assertEquals(136, policy.inputBoxMaxHeightDp)
        assertEquals(220, policy.emojiPanelHeightDp)
        assertEquals(16, policy.sheetHorizontalPaddingDp)
        assertEquals(40, policy.toolbarToolButtonSizeDp)
        assertEquals(6, policy.toolbarToolSpacingDp)
        assertEquals(16, policy.sendButtonHorizontalPaddingDp)
    }

    @Test
    fun landscape_compactsEditorToReduceVideoOcclusion() {
        val portraitPolicy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = false
        )
        val landscapePolicy = resolveCommentInputDialogLayoutPolicy(
            isLandscape = true
        )

        assertEquals(64, landscapePolicy.inputBoxMinHeightDp)
        assertEquals(112, landscapePolicy.inputBoxMaxHeightDp)
        assertEquals(196, landscapePolicy.emojiPanelHeightDp)
        assertEquals(18, landscapePolicy.sendButtonHorizontalPaddingDp)
        assertTrue(landscapePolicy.inputBoxMinHeightDp < portraitPolicy.inputBoxMinHeightDp)
        assertTrue(landscapePolicy.inputBoxMaxHeightDp < portraitPolicy.inputBoxMaxHeightDp)
        assertTrue(landscapePolicy.emojiPanelHeightDp < portraitPolicy.emojiPanelHeightDp)
    }

    @Test
    fun progressInsertText_wrapsFormattedPlaybackTimeWithSpaces() {
        assertEquals(" 01:05 ", resolveCommentProgressInsertText(65_000L))
        assertEquals(" 00:00 ", resolveCommentProgressInsertText(-1L))
    }

    @Test
    fun activeMentionQuery_readsTextAfterLastAtBeforeCursor() {
        val query = resolveActiveCommentMentionQuery("一起 @社会", cursor = 6)

        assertEquals(3, query?.atIndex)
        assertEquals("社会", query?.query)
    }

    @Test
    fun activeMentionQuery_ignoresWhitespaceSeparatedAt() {
        val query = resolveActiveCommentMentionQuery("@社会 易", cursor = 5)

        assertEquals(null, query)
    }

    @Test
    fun mentionInsert_replacesActiveQueryAtCursor() {
        val (text, selection) = insertCommentMentionText(
            text = "一起 @社会",
            cursor = 6,
            mentionName = "社会易姐QwQ"
        )

        assertEquals("一起 @社会易姐QwQ ", text)
        assertEquals(text.length, selection.start)
        assertEquals(text.length, selection.end)
    }

    @Test
    fun mentionPanel_exposesFriendNameSearchField() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentInputDialog.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/video/ui/components/CommentInputDialog.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("placeholder = { Text(\"搜索好友昵称\") }"))
        assertTrue(source.contains("onMentionSearchQueryChange(query)"))
        assertTrue(source.contains("输入好友昵称搜索"))
    }
}
