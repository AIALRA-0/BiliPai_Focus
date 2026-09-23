package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicStatModule

/** Reflect a confirmed repost locally until the next feed refresh returns the server count. */
internal fun applyDynamicForwardCountIncrement(
    items: List<DynamicItem>,
    dynamicId: String
): List<DynamicItem> {
    return items.map { item ->
        if (item.id_str != dynamicId) return@map item

        val statModule = item.modules.module_stat ?: DynamicStatModule()
        item.copy(
            modules = item.modules.copy(
                module_stat = statModule.copy(
                    forward = statModule.forward.copy(count = statModule.forward.count + 1)
                )
            )
        )
    }
}
