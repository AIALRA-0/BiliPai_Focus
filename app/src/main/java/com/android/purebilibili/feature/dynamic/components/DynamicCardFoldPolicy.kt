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

/**
 * Reveal the hidden cards that Bilibili folded under [anchorId], matching PiliPlus
 * `onUnfold`: keep them in the list, flip `visible`, and drop the fold bar.
 */
internal fun unfoldRelatedDynamicItems(
    items: List<DynamicItem>,
    anchorId: String,
): List<DynamicItem> {
    if (items.isEmpty() || anchorId.isBlank()) return items
    val anchorIndex = items.indexOfFirst { it.id_str == anchorId }
    if (anchorIndex < 0) return items
    val fold = items[anchorIndex].modules.module_fold ?: return items
    val foldedIds = fold.ids.map(String::trim).filter(String::isNotBlank).toSet()
    if (foldedIds.isEmpty()) return items
    val positionalEnd = (anchorIndex + foldedIds.size).coerceAtMost(items.lastIndex)
    return items.mapIndexed { index, item ->
        when {
            index == anchorIndex -> item.copy(
                modules = item.modules.copy(module_fold = null),
            )
            item.id_str in foldedIds -> item.copy(visible = true)
            index in (anchorIndex + 1)..positionalEnd && !item.visible -> item.copy(visible = true)
            else -> item
        }
    }
}

// 风险提示条：title 或 desc 任一非空即渲染。
internal fun shouldShowDynamicDispute(dispute: DynamicDisputeModule?): Boolean {
    if (dispute == null) return false
    return dispute.title.isNotBlank() || dispute.desc.isNotBlank()
}
