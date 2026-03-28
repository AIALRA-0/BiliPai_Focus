package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.DEFAULT_FOCUS_FOLLOW_GROUP_ID
import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFollowingSyncPolicyTest {

    @Test
    fun `snapshot change should require reload when visible followings are added`() {
        val change = resolveHomeFollowingSnapshotChange(
            previousFollowingMids = setOf(11L, 22L),
            nextFollowingMids = setOf(11L, 22L, 33L),
            blockedMids = emptySet(),
            config = FocusFollowGroupConfig(),
            filterEnabled = true
        )

        assertEquals(HomeFollowingSnapshotChangeKind.RELOAD_REQUIRED, change.kind)
        assertEquals(setOf(33L), change.addedVisibleMids)
        assertTrue(change.removedVisibleMids.isEmpty())
    }

    @Test
    fun `snapshot change should prune only when visible followings are removed`() {
        val change = resolveHomeFollowingSnapshotChange(
            previousFollowingMids = setOf(11L, 22L, 33L),
            nextFollowingMids = setOf(11L, 22L),
            blockedMids = emptySet(),
            config = FocusFollowGroupConfig(),
            filterEnabled = true
        )

        assertEquals(HomeFollowingSnapshotChangeKind.REMOVED_ONLY, change.kind)
        assertEquals(setOf(33L), change.removedVisibleMids)
        assertTrue(change.addedVisibleMids.isEmpty())
    }

    @Test
    fun `snapshot change should ignore hidden followings when filter is enabled`() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = DEFAULT_FOCUS_FOLLOW_GROUP_ID, name = "默认分组", visible = true),
                FocusFollowGroup(id = "hidden", name = "隐藏", visible = false)
            ),
            assignments = mapOf("33" to "hidden")
        )

        val change = resolveHomeFollowingSnapshotChange(
            previousFollowingMids = setOf(11L, 22L),
            nextFollowingMids = setOf(11L, 22L, 33L),
            blockedMids = emptySet(),
            config = config,
            filterEnabled = true
        )

        assertEquals(HomeFollowingSnapshotChangeKind.NONE, change.kind)
        assertTrue(change.addedVisibleMids.isEmpty())
        assertTrue(change.removedVisibleMids.isEmpty())
    }

    @Test
    fun `follow refresh should stay idle when feed has not been resolved yet`() {
        assertFalse(
            shouldRefreshHomeFollowAfterFollowingChange(
                hasResolvedFollowFeedOnce = false,
                rawFollowFeedCount = 0,
                displayedFollowFeedCount = 0,
                currentCategory = HomeCategory.RECOMMEND
            )
        )
        assertTrue(
            shouldRefreshHomeFollowAfterFollowingChange(
                hasResolvedFollowFeedOnce = false,
                rawFollowFeedCount = 0,
                displayedFollowFeedCount = 0,
                currentCategory = HomeCategory.FOLLOW
            )
        )
    }
}
