package com.android.purebilibili.core.util

/**
 * 将刷新结果按时间顺序插到顶部，并保持现有列表不被清空。
 */
fun <T, K> prependDistinctByKey(
    existing: List<T>,
    incoming: List<T>,
    keySelector: (T) -> K
): List<T> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming.distinctBy(keySelector)

    val seen = existing.asSequence().map(keySelector).toHashSet()
    val prepended = incoming.filter { item ->
        seen.add(keySelector(item))
    }
    return if (prepended.isEmpty()) existing else prepended + existing
}

/**
 * 刷新时以服务端 fresh 批次顺序为准：批次里已存在的项也移动到 fresh 位置。
 */
fun <T, K> promoteDistinctByKey(
    existing: List<T>,
    incoming: List<T>,
    keySelector: (T) -> K
): List<T> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming.distinctBy(keySelector)

    val incomingDistinct = incoming.distinctBy(keySelector)
    val incomingKeys = incomingDistinct.asSequence().map(keySelector).toHashSet()
    return incomingDistinct + existing.filter { item -> keySelector(item) !in incomingKeys }
}

/**
 * 分页加载时只追加新项，避免跨页重复。
 */
fun <T, K> appendDistinctByKey(
    existing: List<T>,
    incoming: List<T>,
    keySelector: (T) -> K
): List<T> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming.distinctBy(keySelector)

    val seen = existing.asSequence().map(keySelector).toHashSet()
    val appended = incoming.filter { item ->
        seen.add(keySelector(item))
    }
    return if (appended.isEmpty()) existing else existing + appended
}
