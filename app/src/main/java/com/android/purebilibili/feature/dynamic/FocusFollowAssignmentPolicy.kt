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

internal fun filterFocusFollowAssignmentSections(
    sections: List<FocusFollowAssignmentSection>,
    query: String
): List<FocusFollowAssignmentSection> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return sections

    return sections.mapNotNull { section ->
        val filteredMembers = section.members.filter { user ->
            val normalizedName = user.uname.trim().lowercase()
            normalizedName.contains(normalizedQuery) ||
                user.mid.toString().contains(normalizedQuery)
        }
        if (filteredMembers.isEmpty()) {
            null
        } else {
            section.copy(members = filteredMembers)
        }
    }
}
