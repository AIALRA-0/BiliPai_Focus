package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.DEFAULT_FOCUS_FOLLOW_GROUP_ID
import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.data.model.response.FollowingUser
import kotlin.test.Test
import kotlin.test.assertEquals

class FocusFollowAssignmentPolicyTest {

    @Test
    fun buildFocusFollowAssignmentSections_keepsConfiguredGroupOrderAndSortedMembers() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = DEFAULT_FOCUS_FOLLOW_GROUP_ID, name = "默认分组", visible = true),
                FocusFollowGroup(id = "friends", name = "朋友", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf(
                "1002" to "friends",
                "1003" to "friends",
                "1004" to "quiet"
            )
        )

        val result = buildFocusFollowAssignmentSections(
            followings = listOf(
                FollowingUser(mid = 1003L, uname = "zeta"),
                FollowingUser(mid = 1002L, uname = "Alpha"),
                FollowingUser(mid = 1001L, uname = "beta"),
                FollowingUser(mid = 1004L, uname = "Gamma")
            ),
            config = config
        )

        assertEquals(
            listOf(DEFAULT_FOCUS_FOLLOW_GROUP_ID, "friends", "quiet"),
            result.map { it.group.id }
        )
        assertEquals(listOf(1001L), result[0].members.map { it.mid })
        assertEquals(listOf(1002L, 1003L), result[1].members.map { it.mid })
        assertEquals(listOf(1004L), result[2].members.map { it.mid })
    }

    @Test
    fun buildFocusFollowAssignmentSections_fallsBackToDefaultWhenTargetGroupIsMissing() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = DEFAULT_FOCUS_FOLLOW_GROUP_ID, name = "默认分组", visible = true),
                FocusFollowGroup(id = "friends", name = "朋友", visible = true)
            ),
            assignments = mapOf("2002" to "missing-group")
        )

        val result = buildFocusFollowAssignmentSections(
            followings = listOf(
                FollowingUser(mid = 2001L, uname = "One"),
                FollowingUser(mid = 2002L, uname = "Two")
            ),
            config = config
        )

        assertEquals(listOf(2001L, 2002L), result.first().members.map { it.mid })
        assertEquals(emptyList(), result.last().members)
    }
}
