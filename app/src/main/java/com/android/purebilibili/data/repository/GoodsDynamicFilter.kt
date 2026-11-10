package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DynamicItem

internal fun isGoodsDynamic(item: DynamicItem): Boolean =
    item.modules.module_dynamic?.additional?.type == "ADDITIONAL_TYPE_GOODS" ||
        item.orig?.let(::isGoodsDynamic) == true

internal fun isGoodsSpaceDynamic(item: com.android.purebilibili.data.model.response.SpaceDynamicItem): Boolean =
    item.modules.module_dynamic?.additional?.type == "ADDITIONAL_TYPE_GOODS" ||
        item.orig?.let(::isGoodsSpaceDynamic) == true

internal suspend fun goodsDynamicFilterEnabled(): Boolean =
    com.android.purebilibili.core.network.NetworkModule.appContext?.let {
        com.android.purebilibili.core.store.ContentDisplaySettings.enabled(
            it, com.android.purebilibili.core.store.ContentDisplaySettings.BLOCK_GOODS_DYNAMIC
        )
    } ?: false

internal suspend fun filterGoodsSpaceDynamics(
    items: List<com.android.purebilibili.data.model.response.SpaceDynamicItem>,
): List<com.android.purebilibili.data.model.response.SpaceDynamicItem> =
    if (goodsDynamicFilterEnabled()) items.filterNot(::isGoodsSpaceDynamic) else items
