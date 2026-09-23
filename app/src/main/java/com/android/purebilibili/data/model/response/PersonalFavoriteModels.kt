package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class FavoriteSection(val label: String) {
    VIDEO("视频"),
    BANGUMI("番剧"),
    CINEMA("影视"),
    ARTICLE("文章"),
    NOTE("笔记"),
    TOPIC("话题"),
    COURSE("课程"),
}

@Serializable
enum class FavoriteSearchScope(val label: String) {
    CURRENT_FOLDER("当前收藏夹"),
    ALL_VIDEO_FOLDERS("全部视频收藏夹"),
}

enum class FavoritePgcStatus(val value: Int, val label: String) {
    WANT(1, "想看"),
    WATCHING(2, "在看"),
    WATCHED(3, "看过"),
}

@Serializable
data class FavoriteArticleResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FavoriteArticleData? = null,
)

@Serializable
data class FavoriteArticleData(
    val items: List<FavoriteArticleItem>? = null,
    @SerialName("has_more") val hasMore: Boolean? = null,
)

@Serializable
data class FavoriteArticleItem(
    @SerialName("opus_id") val opusId: String? = null,
    val content: String? = null,
    val author: FavoriteArticleAuthor? = null,
    val cover: FavoriteArticleCover? = null,
    val stat: FavoriteArticleStat? = null,
    @SerialName("pub_time") val publishTime: String? = null,
)

@Serializable data class FavoriteArticleAuthor(val name: String? = null)
@Serializable data class FavoriteArticleCover(val url: String? = null)
@Serializable data class FavoriteArticleStat(val like: String? = null)

@Serializable
data class FavoriteNoteResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FavoriteNoteData? = null,
)

@Serializable data class FavoriteNoteData(val list: List<FavoriteNoteItem>? = null)

@Serializable
data class FavoriteNoteItem(
    @SerialName("web_url") val webUrl: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val message: String? = null,
    val pic: String? = null,
    val arc: FavoriteNoteArchive? = null,
    val cvid: Long? = null,
    @SerialName("note_id") val noteId: Long? = null,
)

@Serializable data class FavoriteNoteArchive(val pic: String? = null)

@Serializable
data class FavoriteTopicResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FavoriteTopicData? = null,
)

@Serializable data class FavoriteTopicData(@SerialName("topic_list") val topicList: FavoriteTopicList? = null)
@Serializable data class FavoriteTopicList(@SerialName("topic_items") val items: List<FavoriteTopicItem>? = null)
@Serializable data class FavoriteTopicItem(val id: Long? = null, val name: String? = null)

@Serializable
data class FavoriteCourseResponse(
    val code: Int = 0,
    val message: String = "",
    val data: FavoriteCourseData? = null,
)

@Serializable data class FavoriteCourseData(val items: List<FavoriteCourseItem>? = null)

@Serializable
data class FavoriteCourseItem(
    val cover: String? = null,
    val marks: List<String>? = null,
    @SerialName("season_id") val seasonId: Long? = null,
    val status: String? = null,
    val title: String? = null,
    val ctime: String? = null,
)
