package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SubReplyDetailLayoutPolicyTest {

    @Test
    fun `detail sheet should not reserve space for a docked composer`() {
        val policy = resolveSubReplyDetailLayoutPolicy(showRootCommentEntry = true)

        assertFalse(policy.overlayRootCommentEntry)
        assertEquals(16, policy.listBottomPaddingDp)
        assertEquals(0, policy.footerTopPaddingDp)
    }

    @Test
    fun `detail sheet keeps compact bottom padding when footer is hidden`() {
        val policy = resolveSubReplyDetailLayoutPolicy(showRootCommentEntry = false)

        assertFalse(policy.overlayRootCommentEntry)
        assertEquals(16, policy.listBottomPaddingDp)
        assertEquals(0, policy.footerTopPaddingDp)
    }
}
