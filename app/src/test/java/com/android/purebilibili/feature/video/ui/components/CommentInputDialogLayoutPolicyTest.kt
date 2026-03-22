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

        assertEquals(100, policy.inputBoxMinHeightDp)
        assertEquals(180, policy.inputBoxMaxHeightDp)
        assertEquals(280, policy.emojiPanelHeightDp)
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
        assertTrue(landscapePolicy.inputBoxMinHeightDp < portraitPolicy.inputBoxMinHeightDp)
        assertTrue(landscapePolicy.inputBoxMaxHeightDp < portraitPolicy.inputBoxMaxHeightDp)
        assertTrue(landscapePolicy.emojiPanelHeightDp < portraitPolicy.emojiPanelHeightDp)
    }
}
