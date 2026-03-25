package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusFollowGroupStorePolicyTest {

    @Test
    fun normalizeConfig_keepsPublishTimeDescendingHomeFeedSortModeByDefault() {
        val result = normalizeFocusFollowGroupConfig(FocusFollowGroupConfig())

        assertEquals(FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC, result.homeFeedSortMode)
    }

    @Test
    fun normalizeConfig_keepsDefaultGroupAndDropsInvalidAssignments() {
        val result = normalizeFocusFollowGroupConfig(
            FocusFollowGroupConfig(
                groups = listOf(
                    FocusFollowGroup(
                        id = "custom",
                        name = "创作者",
                        visible = true
                    )
                ),
                assignments = mapOf(
                    "1001" to "custom",
                    "1002" to "missing",
                    "oops" to "custom"
                )
            )
        )

        assertEquals(DEFAULT_FOCUS_FOLLOW_GROUP_ID, result.groups.first().id)
        assertEquals(2, result.groups.size)
        assertEquals("custom", result.assignments["1001"])
        assertFalse(result.assignments.containsKey("1002"))
        assertFalse(result.assignments.containsKey("oops"))
    }

    @Test
    fun assignUserToGroup_keepsSingleGroupOwnership() {
        val source = withFocusFollowGroupCreated(
            config = FocusFollowGroupConfig(),
            name = "游戏区",
            idProvider = { "game" }
        )

        val assigned = withUserAssignedToFocusFollowGroup(
            config = source,
            mid = 2233L,
            groupId = "game"
        )
        val resetToDefault = withUserAssignedToFocusFollowGroup(
            config = assigned,
            mid = 2233L,
            groupId = DEFAULT_FOCUS_FOLLOW_GROUP_ID
        )

        assertEquals("game", resolveFocusFollowGroupIdForUser(assigned, 2233L))
        assertFalse(resetToDefault.assignments.containsKey("2233"))
        assertEquals(DEFAULT_FOCUS_FOLLOW_GROUP_ID, resolveFocusFollowGroupIdForUser(resetToDefault, 2233L))
    }

    @Test
    fun deleteGroup_movesMembersBackToDefault() {
        val created = withFocusFollowGroupCreated(
            config = FocusFollowGroupConfig(),
            name = "朋友",
            idProvider = { "friends" }
        )
        val assigned = withUserAssignedToFocusFollowGroup(
            config = created,
            mid = 3344L,
            groupId = "friends"
        )

        val deleted = withFocusFollowGroupDeleted(assigned, "friends")

        assertTrue(deleted.groups.none { it.id == "friends" })
        assertFalse(deleted.assignments.containsKey("3344"))
        assertTrue(isFocusFollowUserVisible(deleted, 3344L))
    }

    @Test
    fun groupVisibility_controlsUserVisibility() {
        val created = withFocusFollowGroupCreated(
            config = FocusFollowGroupConfig(),
            name = "静音观察",
            idProvider = { "muted" }
        )
        val assigned = withUserAssignedToFocusFollowGroup(
            config = created,
            mid = 5566L,
            groupId = "muted"
        )
        val hidden = withFocusFollowGroupVisibility(
            config = assigned,
            groupId = "muted",
            visible = false
        )

        assertFalse(isFocusFollowUserVisible(hidden, 5566L))
        assertTrue(isFocusFollowUserVisible(hidden, 8899L))
    }

    @Test
    fun homeFeedSortMode_updatesWithoutLosingGroupsOrAssignments() {
        val created = withFocusFollowGroupCreated(
            config = FocusFollowGroupConfig(),
            name = "朋友",
            idProvider = { "friends" }
        )
        val assigned = withUserAssignedToFocusFollowGroup(
            config = created,
            mid = 7788L,
            groupId = "friends"
        )

        val updated = withFocusFollowHomeFeedSortMode(
            config = assigned,
            sortMode = FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC
        )

        assertEquals(FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC, updated.homeFeedSortMode)
        assertEquals("friends", resolveFocusFollowGroupIdForUser(updated, 7788L))
        assertTrue(updated.groups.any { it.id == "friends" })
    }
}
