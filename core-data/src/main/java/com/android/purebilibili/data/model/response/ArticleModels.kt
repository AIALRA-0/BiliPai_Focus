package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Serializable
data class ArticleDetailResponse(
    val code: Int = 0,
    val message: String = "",
    val data: ArticleViewData? = null
)

@Serializable
data class ArticleViewData(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    @SerialName("banner_url")
    val bannerUrl: String = "",
    val author: ArticleAuthor? = null,
    @SerialName("publish_time")
    @Serializable(with = FlexibleLongSerializer::class)
    val publishTime: Long = 0,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("origin_image_urls")
    val originImageUrls: List<String> = emptyList(),
    @SerialName("dyn_id_str")
    val dynamicId: String = "",
    @Serializable(with = FlexibleIntSerializer::class)
    val type: Int = 0,
    val content: String = "",
    val opus: ArticleOpus? = null,
    val ops: List<JsonObject> = emptyList(),
    val list: ArticleCollectionInfo? = null,
    val stats: ArticleStats? = null,
)

@Serializable
data class ArticleAuthor(
    @Serializable(with = FlexibleLongSerializer::class)
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class ArticleOpus(
    val title: String = "",
    val content: JsonObject? = null
) {
    fun paragraphs(): List<JsonObject> {
        val paragraphs = content?.get("paragraphs")?.jsonArray ?: return emptyList()
        return paragraphs.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    }
}

@Serializable
data class ArticleInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: ArticleInfoData? = null,
)

@Serializable
data class ArticleInfoData(
    @Serializable(with = FlexibleBooleanSerializer::class)
    val like: Boolean = false,
    val favorite: Boolean = false,
    val stats: ArticleStats? = null,
)

@Serializable
data class ArticleStats(
    val view: Long? = null,
    val like: Long? = null,
    val favorite: Long? = null,
    val reply: Long? = null,
    val share: Long? = null,
)

@Serializable
data class ArticleCollectionInfo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val summary: String = "",
    @SerialName("articles_count") val articlesCount: Int? = null,
    val words: Long? = null,
    val read: Long? = null,
    @SerialName("update_time") val updateTime: Long? = null,
)

@Serializable
data class ArticleCollectionResponse(
    val code: Int = 0,
    val message: String = "",
    val data: ArticleCollectionData? = null,
)

@Serializable
data class ArticleCollectionData(
    val list: ArticleCollectionInfo? = null,
    val author: ArticleAuthor? = null,
    val articles: List<ArticleCollectionItem> = emptyList(),
)

@Serializable
data class ArticleCollectionItem(
    val id: Long = 0,
    val title: String = "",
    val summary: String = "",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val stats: ArticleStats? = null,
)
