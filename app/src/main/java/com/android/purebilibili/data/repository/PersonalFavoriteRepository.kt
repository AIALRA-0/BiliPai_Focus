package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.FavoriteSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FavoriteCategoryItem(
    val id: Long,
    val title: String,
    val subtitle: String = "",
    val cover: String = "",
    val url: String = "",
    val badge: String = "",
    val section: FavoriteSection,
)

data class FavoriteCategoryPage(
    val items: List<FavoriteCategoryItem>,
    val hasMore: Boolean,
)

object PersonalFavoriteRepository {
    private val api = NetworkModule.api

    suspend fun getArticles(page: Int): Result<FavoriteCategoryPage> = withContext(Dispatchers.IO) {
        apiCall {
            val response = api.getFavoriteArticles(page = page)
            check(response.code == 0) { response.message.ifBlank { "加载文章收藏失败" } }
            val data = response.data
            FavoriteCategoryPage(
                items = data?.items.orEmpty().mapNotNull { article ->
                    val id = article.opusId?.toLongOrNull() ?: return@mapNotNull null
                    FavoriteCategoryItem(
                        id = id,
                        title = article.content.orEmpty().lineSequence().firstOrNull().orEmpty().ifBlank { "收藏文章" },
                        subtitle = listOfNotNull(
                            article.author?.name?.takeIf(String::isNotBlank),
                            article.publishTime?.takeIf(String::isNotBlank),
                            article.stat?.like?.takeIf(String::isNotBlank)?.let { "${it}赞" },
                        ).joinToString(" · "),
                        cover = article.cover?.url.orEmpty(),
                        section = FavoriteSection.ARTICLE,
                    )
                },
                hasMore = data?.hasMore == true,
            )
        }
    }

    suspend fun getNotes(page: Int, published: Boolean): Result<FavoriteCategoryPage> = withContext(Dispatchers.IO) {
        apiCall {
            val csrf = requireCsrf()
            val response = if (published) {
                api.getPublishedFavoriteNotes(page = page, csrf = csrf)
            } else {
                api.getFavoriteNotes(page = page, csrf = csrf)
            }
            check(response.code == 0) { response.message.ifBlank { "加载笔记失败" } }
            val items = response.data?.list.orEmpty().mapNotNull { note ->
                val id = note.noteId ?: note.cvid ?: return@mapNotNull null
                FavoriteCategoryItem(
                    id = id,
                    title = note.title.orEmpty().ifBlank { "视频笔记" },
                    subtitle = note.summary.orEmpty().ifBlank { note.message.orEmpty() },
                    cover = note.pic.orEmpty().ifBlank { note.arc?.pic.orEmpty() },
                    url = note.webUrl.orEmpty(),
                    badge = if (published) "公开" else "未发布",
                    section = FavoriteSection.NOTE,
                )
            }
            FavoriteCategoryPage(items = items, hasMore = items.size >= 10)
        }
    }

    suspend fun getTopics(page: Int): Result<FavoriteCategoryPage> = withContext(Dispatchers.IO) {
        apiCall {
            val response = api.getFavoriteTopics(page = page)
            check(response.code == 0) { response.message.ifBlank { "加载话题收藏失败" } }
            val items = response.data?.topicList?.items.orEmpty().mapNotNull { topic ->
                val id = topic.id ?: return@mapNotNull null
                FavoriteCategoryItem(
                    id = id,
                    title = topic.name.orEmpty().ifBlank { "话题$id" },
                    section = FavoriteSection.TOPIC,
                )
            }
            FavoriteCategoryPage(items = items, hasMore = items.size >= 24)
        }
    }

    suspend fun getCourses(page: Int): Result<FavoriteCategoryPage> = withContext(Dispatchers.IO) {
        apiCall {
            val mid = TokenManager.midCache ?: error("请先登录")
            val response = api.getFavoriteCourses(mid = mid, page = page)
            check(response.code == 0) { response.message.ifBlank { "加载课程收藏失败" } }
            val items = response.data?.items.orEmpty().mapNotNull { course ->
                val id = course.seasonId ?: return@mapNotNull null
                FavoriteCategoryItem(
                    id = id,
                    title = course.title.orEmpty().ifBlank { "课程$id" },
                    subtitle = listOfNotNull(
                        course.status?.takeIf(String::isNotBlank),
                        course.ctime?.takeIf(String::isNotBlank),
                    ).joinToString(" · "),
                    cover = course.cover.orEmpty(),
                    url = "https://www.bilibili.com/cheese/play/ss$id",
                    badge = course.marks.orEmpty().firstOrNull().orEmpty(),
                    section = FavoriteSection.COURSE,
                )
            }
            FavoriteCategoryPage(items = items, hasMore = items.size >= 20)
        }
    }

    suspend fun remove(item: FavoriteCategoryItem, publishedNote: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            apiCall {
                val csrf = requireCsrf()
                val response = when (item.section) {
                    FavoriteSection.ARTICLE -> api.deleteFavoriteArticle(item.id, csrf)
                    FavoriteSection.NOTE -> if (publishedNote) {
                        api.deletePublishedFavoriteNote(item.id.toString(), csrf)
                    } else {
                        api.deleteFavoriteNote(item.id.toString(), csrf)
                    }
                    FavoriteSection.TOPIC -> api.deleteFavoriteTopic(item.id, csrf)
                    FavoriteSection.COURSE -> api.deleteFavoriteCourse(item.id, csrf)
                    else -> error("该分类不支持此操作")
                }
                check(response.code == 0) { response.message.ifBlank { "移除收藏失败" } }
            }
        }

    private fun requireCsrf(): String = TokenManager.csrfCache.orEmpty().ifBlank { error("请先登录") }

    private suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
