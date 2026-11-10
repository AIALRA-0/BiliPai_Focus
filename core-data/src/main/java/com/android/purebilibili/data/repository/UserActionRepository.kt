package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.FavFolder

object UserActionRepository {
    private fun csrf() = TokenManager.csrfCache?.takeIf { it.isNotBlank() } ?: throw ContentRequestException(-101, "")
    suspend fun like(aid: Long, selected: Boolean): Result<Boolean> = dataRequest {
        val response = NetworkModule.api.likeVideo(aid, if (selected) 1 else 2, csrf())
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        true
    }
    suspend fun liked(aid: Long): Result<Boolean> = dataRequest {
        val response = NetworkModule.api.hasLiked(aid)
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        response.data == 1
    }
    suspend fun follow(mid: Long, selected: Boolean): Result<Boolean> = dataRequest {
        val response = NetworkModule.api.modifyRelation(mid, if (selected) 1 else 2, csrf())
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        true
    }
    suspend fun following(mid: Long): Result<Boolean> = dataRequest {
        val response = NetworkModule.api.getRelation(mid)
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        response.data?.isFollowing ?: false
    }
    suspend fun folders(aid: Long? = null): Result<List<FavFolder>> = dataRequest {
        val mid = TokenManager.midCache ?: throw ContentRequestException(-101, "")
        val response = NetworkModule.api.getFavFolders(mid, type = aid?.let { 2 }, rid = aid)
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        response.data?.list.orEmpty()
    }
    suspend fun favorite(aid: Long, add: Set<Long>, remove: Set<Long>): Result<Boolean> = dataRequest {
        check(add.intersect(remove).isEmpty()) { "收藏夹操作冲突" }
        val response = NetworkModule.api.dealFavorite(rid = aid, addIds = add.sorted().joinToString(","),
            delIds = remove.sorted().joinToString(","), csrf = csrf())
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        true
    }

    /** 投币：count 1-2，alsoLike 同时点赞；错误码文案与移动端 ActionRepository 保持一致。 */
    suspend fun coin(aid: Long, count: Int, alsoLike: Boolean): Result<Boolean> = dataRequest {
        val response = NetworkModule.api.coinVideo(
            aid = aid, multiply = count.coerceIn(1, 2), selectLike = if (alsoLike) 1 else 0, csrf = csrf(),
        )
        if (response.code != 0) {
            val message = when (response.code) {
                34004 -> "操作太频繁，请稍后重试"
                34005 -> "已投满 2 个硬币"
                -104 -> "硬币余额不足"
                else -> response.message.ifBlank { "投币失败" }
            }
            throw ContentRequestException(response.code, message)
        }
        true
    }
}
