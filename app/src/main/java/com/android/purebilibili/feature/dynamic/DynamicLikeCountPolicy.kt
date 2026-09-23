package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicStatModule

internal class DynamicLikeRequestGate {
    private val activeDynamicIds = mutableSetOf<String>()

    fun tryAcquire(dynamicId: String): Boolean = synchronized(activeDynamicIds) {
        activeDynamicIds.add(dynamicId)
    }

    fun release(dynamicId: String) {
        synchronized(activeDynamicIds) {
            activeDynamicIds.remove(dynamicId)
        }
    }
}

internal fun resolveDynamicLikeState(
    localOverride: Boolean?,
    localLiked: Boolean,
    serverLiked: Boolean?,
): Boolean = localOverride ?: (localLiked || serverLiked == true)

internal fun applyDynamicLikeCountChange(
    items: List<DynamicItem>,
    dynamicId: String,
    toLiked: Boolean
): List<DynamicItem> {
    return items.map { item ->
        if (item.id_str != dynamicId) return@map item

        val statModule = item.modules.module_stat ?: DynamicStatModule()
        val currentCount = statModule.like.count
        val updatedCount = if (toLiked) {
            currentCount + 1
        } else {
            (currentCount - 1).coerceAtLeast(0)
        }

        item.copy(
            modules = item.modules.copy(
                module_stat = statModule.copy(
                    like = statModule.like.copy(count = updatedCount, status = toLiked)
                )
            )
        )
    }
}
