package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem

data class DynamicDeleteAction(
    val dynamicId: String,
    val dynType: Int?,
    val rid: String?,
    val label: String,
    val title: String,
    val content: String,
    val confirmText: String,
    val cancelText: String
)

internal fun resolveDynamicDeleteAction(item: DynamicItem): DynamicDeleteAction? {
    val deleteItem = item.modules.module_more
        ?.three_point_items
        ?.firstOrNull { it.type == "THREE_POINT_DELETE" }
        ?: return null
    val params = deleteItem.params
    val dynamicId = params?.dyn_id_str
        ?.takeIf { it.isNotBlank() }
        ?: item.id_str.takeIf { it.isNotBlank() }
        ?: return null

    return DynamicDeleteAction(
        dynamicId = dynamicId,
        dynType = params?.dyn_type?.takeIf { it > 0 },
        rid = params?.rid_str?.takeIf { it.isNotBlank() },
        label = deleteItem.label.takeIf { it.isNotBlank() } ?: "删除",
        title = deleteItem.modal?.title?.takeIf { it.isNotBlank() } ?: "要删除动态吗？",
        content = deleteItem.modal?.content?.takeIf { it.isNotBlank() }
            ?: "动态删除后将无法恢复，请谨慎操作",
        confirmText = deleteItem.modal?.confirm?.takeIf { it.isNotBlank() } ?: "删除",
        cancelText = deleteItem.modal?.cancel?.takeIf { it.isNotBlank() } ?: "取消"
    )
}
