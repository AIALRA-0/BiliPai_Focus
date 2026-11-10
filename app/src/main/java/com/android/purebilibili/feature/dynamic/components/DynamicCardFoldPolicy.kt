package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicDisputeModule
import com.android.purebilibili.data.model.response.DynamicFoldModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicTagModule

/**
 * 卡片附加模块的渲染策略：相关动态折叠条、置顶标、风险提示条。
 * 对齐 BiliPai 的 moduleFold / moduleTag / moduleDispute 处理。
 */

// 置顶标：module_tag 存在且 text 非空（B 站固定返回 "置顶"）时显示。
internal fun shouldShowDynamicPinnedTag(tag: DynamicTagModule?): Boolean =
    !tag?.text.isNullOrBlank()

// 相关动态折叠条：statement 非空（如 "展开3条相关动态"）才渲染。
internal fun resolveDynamicFoldStatement(fold: DynamicFoldModule?): String? {
    if (fold == null) return null
    val statement = fold.statement.trim()
    return statement.ifEmpty { null }
}

internal fun resolveDynamicFoldIds(item: DynamicItem): List<String> =
    item.modules.module_fold?.ids.orEmpty()
        .map(String::trim)
        .filter { it.isNotBlank() && it != item.id_str }
        .distinct()

/** Expand exact IDs only; missing cards keep a retry entry instead of silently disappearing. */
internal fun unfoldRelatedDynamicItems(
    items: List<DynamicItem>,
    anchorId: String,
    loadedItems: List<DynamicItem> = emptyList(),
    resolvedFold: DynamicFoldModule? = null,
): List<DynamicItem> {
    if (items.isEmpty() || anchorId.isBlank()) return items
    val anchorIndex = items.indexOfFirst { it.id_str == anchorId }
    if (anchorIndex < 0) return items
    val anchor = items[anchorIndex]
    val originalFold = anchor.modules.module_fold ?: return items
    val fold = resolvedFold ?: originalFold
    val foldedIds = resolveDynamicFoldIds(anchor.copy(modules = anchor.modules.copy(module_fold = fold)))
    if (foldedIds.isEmpty()) return items

    // The live list wins over downloaded snapshots (likes and other interactions may change).
    val available = (loadedItems + items).associateBy { it.id_str }
    val related = foldedIds.mapNotNull { available[it]?.copy(visible = true) }
    if (related.isEmpty()) return items
    val remainingIds = foldedIds.filterNot(available::containsKey)
    val nextFold = if (remainingIds.isEmpty()) null else fold.copy(
        // Keep the full group order; retries fetch missing IDs but must not move them
        // ahead of cards that were already revealed by a partial success.
        ids = foldedIds,
        statement = "重试加载${remainingIds.size}条相关动态",
    )
    val relatedIds = related.map { it.id_str }.toSet()
    return buildList {
        items.forEach { item ->
            when {
                item.id_str == anchorId -> {
                    add(item.copy(modules = item.modules.copy(module_fold = nextFold)))
                    addAll(related)
                }
                item.id_str !in relatedIds -> add(item)
            }
        }
    }
}

// 风险提示条：title 或 desc 任一非空即渲染。
internal fun shouldShowDynamicDispute(dispute: DynamicDisputeModule?): Boolean {
    if (dispute == null) return false
    return dispute.title.isNotBlank() || dispute.desc.isNotBlank()
}
