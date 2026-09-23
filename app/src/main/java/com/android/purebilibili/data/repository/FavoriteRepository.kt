package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class FavoriteRequestException(
    val apiCode: Int? = null,
    val httpCode: Int? = null,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

private fun favoriteApiFailure(
    operation: String,
    code: Int,
    message: String
): FavoriteRequestException {
    val detail = message.ifBlank { "未知错误" }
    return FavoriteRequestException(
        apiCode = code,
        message = "$operation: $code $detail"
    )
}

private fun favoriteHttpFailure(
    operation: String,
    exception: HttpException
): FavoriteRequestException {
    return FavoriteRequestException(
        httpCode = exception.code(),
        message = "$operation: HTTP ${exception.code()}",
        cause = exception
    )
}

internal data class FavoriteResourceRequestParams(
    val page: Int,
    val pageSize: Int,
    val keyword: String,
    val order: String,
    val type: Int,
    val tid: Int,
    val platform: String,
)

internal fun resolveFavoriteResourceRequestParams(
    pn: Int,
    ps: Int,
    keyword: String?,
    order: String?,
    type: Int,
    tid: Int,
    platform: String,
): FavoriteResourceRequestParams = FavoriteResourceRequestParams(
    page = pn.coerceAtLeast(1),
    pageSize = ps.coerceIn(1, 20),
    keyword = keyword.orEmpty(),
    order = order?.takeIf(String::isNotBlank) ?: "mtime",
    type = type,
    tid = tid,
    platform = platform.ifBlank { "web" },
)

object FavoriteRepository {
    private val api = NetworkModule.api

    data class CollectedFavFoldersPage(
        val folders: List<FavFolder>,
        val totalCount: Int
    )

    suspend fun getFavFolders(mid: Long): Result<List<FavFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getFavFolders(mid)
                if (response.code == 0) {
                    Result.success(
                        response.data?.list
                            ?.map { it.copy(source = FavFolderSource.OWNED) }
                            ?: emptyList()
                    )
                } else {
                    Result.failure(
                        favoriteApiFailure(
                            operation = "获取收藏夹失败",
                            code = response.code,
                            message = response.message
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                Result.failure(favoriteHttpFailure("获取收藏夹失败", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun editFolder(
        mediaId: Long,
        title: String,
        intro: String,
        isPrivate: Boolean,
        cover: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
            val response = api.editFavFolder(
                mediaId = mediaId,
                title = title,
                intro = intro,
                privacy = if (isPrivate) 1 else 0,
                cover = cover,
                csrf = csrf,
            )
            if (response.code == 0) Result.success(Unit)
            else Result.failure(Exception(response.message.ifBlank { "编辑收藏夹失败" }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(mediaId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
            val response = api.deleteFavFolders(mediaIds = mediaId.toString(), csrf = csrf)
            if (response.code == 0) Result.success(Unit)
            else Result.failure(Exception(response.message.ifBlank { "删除收藏夹失败" }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCollectedFavFolders(
        mid: Long,
        pn: Int = 1,
        ps: Int = 20,
        platform: String = "web"
    ): Result<CollectedFavFoldersPage> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCollectedFavFolders(mid = mid, pn = pn, ps = ps, platform = platform)
                if (response.code == 0) {
                    Result.success(
                        CollectedFavFoldersPage(
                            folders = response.data?.list
                                ?.map { it.copy(source = FavFolderSource.SUBSCRIBED) }
                                ?: emptyList(),
                            totalCount = response.data?.count ?: 0
                        )
                    )
                } else {
                    Result.failure(
                        favoriteApiFailure(
                            operation = "获取收藏合集失败",
                            code = response.code,
                            message = response.message
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                Result.failure(favoriteHttpFailure("获取收藏合集失败", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFavoriteList(
        mediaId: Long,
        pn: Int,
        ps: Int = 20,
        keyword: String? = null,
        order: String? = null,
        type: Int = 0,
        tid: Int = 0,
        platform: String = "web"
    ): Result<FavoriteResourceData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = resolveFavoriteResourceRequestParams(
                    pn = pn,
                    ps = ps,
                    keyword = keyword,
                    order = order,
                    type = type,
                    tid = tid,
                    platform = platform,
                )
                val response = api.getFavoriteList(
                    mediaId = mediaId,
                    pn = request.page,
                    ps = request.pageSize,
                    keyword = request.keyword,
                    order = request.order,
                    type = request.type,
                    tid = request.tid,
                    platform = request.platform,
                )
                if (response.code == 0 && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(
                        favoriteApiFailure(
                            operation = "获取收藏夹内容失败",
                            code = response.code,
                            message = response.message
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                Result.failure(favoriteHttpFailure("获取收藏夹内容失败", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFavoriteSeasonList(
        seasonId: Long,
        pn: Int
    ): Result<FavoriteResourceData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getFavoriteSeasonList(
                    seasonId = seasonId,
                    pn = pn
                )
                if (response.code == 0 && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun cleanInvalidResources(mediaId: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
                if (csrf.isBlank()) {
                    return@withContext Result.failure(Exception("请先登录"))
                }
                val response = api.cleanInvalidFavResource(mediaId = mediaId, csrf = csrf)
                if (response.code == 0) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "清理失效内容失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun removeResource(mediaId: Long, resourceId: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
                // type=2 代表视频
                val resourceStr = "$resourceId:2"
                val response = api.batchDelFavResource(mediaId, resourceStr, csrf)
                
                if (response.code == 0) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun removeResources(mediaId: Long, resourceIds: Set<Long>): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (resourceIds.isEmpty()) return@withContext Result.success(Unit)
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
                if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
                val response = api.batchDelFavResource(
                    mediaId = mediaId,
                    resources = resourceIds.joinToString(",") { "$it:2" },
                    csrf = csrf,
                )
                if (response.code == 0) Result.success(Unit)
                else Result.failure(Exception(response.message.ifBlank { "批量删除失败" }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun copyOrMoveResources(
        sourceMediaId: Long,
        targetMediaId: Long,
        mid: Long,
        resourceIds: Set<Long>,
        copy: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (resourceIds.isEmpty()) return@withContext Result.success(Unit)
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
            val resources = resourceIds.joinToString(",") { "$it:2" }
            val response = if (copy) {
                api.copyFavResources(sourceMediaId, targetMediaId, mid, resources, csrf = csrf)
            } else {
                api.moveFavResources(sourceMediaId, targetMediaId, mid, resources, csrf = csrf)
            }
            if (response.code == 0) Result.success(Unit)
            else Result.failure(Exception(response.message.ifBlank { if (copy) "复制失败" else "移动失败" }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareFolderToDynamic(
        mediaId: Long,
        content: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
            val response = NetworkModule.dynamicApi.repostDynamic(
                csrf = csrf,
                body = com.android.purebilibili.core.network.buildFavoriteFolderDynamicRequest(
                    mediaId = mediaId,
                    content = content,
                ),
            )
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifBlank { "分享至动态失败" }))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
