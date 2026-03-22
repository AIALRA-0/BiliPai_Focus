package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.resolveFocusFollowGroupIdForUser
import com.android.purebilibili.data.model.response.FollowingUser

internal data class FocusFollowAssignmentSection(
    val group: FocusFollowGroup,
    val members: List<FollowingUser>
)

internal fun buildFocusFollowAssignmentSections(
    followings: List<FollowingUser>,
    config: FocusFollowGroupConfig
): List<FocusFollowAssignmentSection> {
    val groupedMembers = followings
        .groupBy { following -> resolveFocusFollowGroupIdForUser(config, following.mid) }
        .mapValues { (_, members) ->
            members.sortedWith(
                compareBy<FollowingUser>(
                    { it.uname.lowercase() },
                    { it.mid }
                )
            )
        }

    return config.groups.map { group ->
        FocusFollowAssignmentSection(
            group = group,
            members = groupedMembers[group.id].orEmpty()
        )
    }
}
