package com.android.purebilibili.feature.following

import com.android.purebilibili.data.model.response.RelationTagItem

internal fun isSortableFollowGroup(id: Long): Boolean = id > 0L

internal fun moveFollowGroup(groups: List<RelationTagItem>, fromId: Long, toId: Long): List<RelationTagItem> {
    if (!isSortableFollowGroup(fromId) || !isSortableFollowGroup(toId) || fromId == toId) return groups
    val custom = groups.filter { isSortableFollowGroup(it.tagid) }
    val from = custom.indexOfFirst { it.tagid == fromId }
    val to = custom.indexOfFirst { it.tagid == toId }
    if (from < 0 || to < 0) return groups
    val moved = custom.toMutableList().apply { add(to, removeAt(from)) }.iterator()
    return groups.map { if (isSortableFollowGroup(it.tagid)) moved.next() else it }
}

internal fun applyFollowGroupOrder(groups: List<RelationTagItem>, order: List<Long>): List<RelationTagItem> {
    val positions = order.withIndex().associate { it.value to it.index }
    val unique = groups.distinctBy { it.tagid }
    return unique.filterNot { isSortableFollowGroup(it.tagid) } +
        unique.filter { isSortableFollowGroup(it.tagid) }.sortedBy { positions[it.tagid] ?: Int.MAX_VALUE }
}

/** 保存后的自定义分组集合以最新读取结果为准，旧缓存仅保留系统分组。 */
internal fun replaceSavedFollowGroups(cached: List<RelationTagItem>, ordered: List<RelationTagItem>): List<RelationTagItem> =
    (cached.filterNot { isSortableFollowGroup(it.tagid) } + ordered.filter { isSortableFollowGroup(it.tagid) })
        .distinctBy { it.tagid }
