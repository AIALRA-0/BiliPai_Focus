package com.android.purebilibili.feature.following

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowingLayoutPolicyTest {
    @Test
    fun listWidth_isCappedForLargeScreens() {
        assertEquals(720.dp, resolveFollowingListMaxWidth())
    }

    @Test
    fun batchGroupDialogHeight_isOwnedByFollowingLayoutPolicy() {
        assertEquals(320.dp, resolveFollowingBatchGroupDialogMaxHeight())
    }
}
