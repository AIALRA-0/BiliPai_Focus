package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import com.android.purebilibili.data.model.response.DynamicItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class DynamicSaveImageSpec(
    val authorName: String,
    val title: String,
    val body: String,
    val dynamicUrl: String,
    val generatedAt: String,
)

internal fun buildDynamicSaveImageSpec(
    item: DynamicItem,
    generatedAtMillis: Long = System.currentTimeMillis(),
): DynamicSaveImageSpec {
    val content = item.modules.module_dynamic
    val body = content?.desc?.text
        .orEmpty()
        .ifBlank { content?.major?.opus?.summary?.text.orEmpty() }
        .ifBlank { "（该动态没有文字内容）" }
    return DynamicSaveImageSpec(
        authorName = item.modules.module_author?.name.orEmpty().ifBlank { "未知用户" },
        title = content?.major?.opus?.title.orEmpty(),
        body = body,
        dynamicUrl = "https://t.bilibili.com/${item.id_str}",
        generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(generatedAtMillis)),
    )
}

suspend fun saveDynamicImageToGallery(context: Context, item: DynamicItem): Boolean = withContext(Dispatchers.IO) {
    try {
        val fullItem = com.android.purebilibili.data.repository.DynamicRepository.getDynamicDetail(item.id_str).getOrThrow()
        com.android.purebilibili.feature.share.saveShareImageDocument(context, com.android.purebilibili.feature.share.dynamicShareDocument(fullItem))
    } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { false }
}
