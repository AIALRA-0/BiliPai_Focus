package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.EmotePackage
import com.android.purebilibili.data.model.response.MessageLikeDetailData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

internal object MessageManagementRepository {
    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.onFailure { if (it is CancellationException) throw it }
    }
    suspend fun contacts(kind: String, page: Int, query: String = ""): Result<List<com.android.purebilibili.data.model.response.FollowingUser>> = io {
        val mid = requireNotNull(TokenManager.midCache?.takeIf { it > 0 }) { "请先登录" }
        if (kind == "blacklist") {
            val response = NetworkModule.api.getRelationBlacks(pageSize = 50, page = page)
            check(response.code == 0) { response.message.ifBlank { "加载黑名单失败" } }
            response.data.list
        } else {
            val response = when {
                query.isNotBlank() -> NetworkModule.messageApi.searchMessageContacts(mid, query, page)
                kind == "fans" -> NetworkModule.api.getFollowers(mid, pn = page, ps = 50)
                else -> NetworkModule.api.getFollowings(mid, pn = page, ps = 50)
            }
            check(response.code == 0) { response.message.ifBlank { "加载通讯录失败" } }
            response.data?.list.orEmpty()
        }
    }
    suspend fun likeDetail(id: Long, page: Int, lastMid: Long): Result<MessageLikeDetailData> = io {
        val response = NetworkModule.messageApi.getLikeDetail(id, page, lastMid)
        check(response.code == 0) { response.message.ifBlank { "加载点赞详情失败" } }
        requireNotNull(response.data) { "点赞详情为空" }
    }
    suspend fun deleteSystemNotice(id: Long): Result<Unit> = io {
        val csrf = requireNotNull(TokenManager.csrfCache?.takeIf { it.isNotBlank() }) { "请先登录" }
        val response = NetworkModule.messageApi.deleteSystemNotice(csrf = csrf, body = buildJsonObject {
            put("csrf", csrf)
            put("ids", buildJsonArray { add(id) })
            put("station_ids", buildJsonArray {})
            put("type", 4)
            put("mobi_app", "android")
        })
        check(response.code == 0) { response.message.ifBlank { "删除通知失败" } }
    }
    suspend fun emotes(): Result<List<EmotePackage>> = com.android.purebilibili.data.repository.CommentRepository.getEmotePackages()
    suspend fun block(mid: Long, blocked: Boolean): Result<Unit> = io {
        val csrf = requireNotNull(TokenManager.csrfCache?.takeIf { it.isNotBlank() }) { "请先登录" }
        val response = NetworkModule.api.modifyRelation(fid = mid, act = if (blocked) 5 else 6, csrf = csrf)
        check(response.code == 0) { response.message.ifBlank { "更新黑名单失败" } }
    }
    suspend fun report(mid: Long, messageKey: Long?, reason: Int, description: String): Result<Unit> = io {
        val csrf = requireNotNull(TokenManager.csrfCache?.takeIf { it.isNotBlank() }) { "请先登录" }
        val response = NetworkModule.messageApi.reportPrivateMessage(mapOf(
            "biz_code" to "4", "accused_uid" to mid.toString(), "object_id" to mid.toString(),
            "reason_type" to reason.toString(), "reason_desc" to description, "module" to "604", "csrf" to csrf,
            "comment" to buildJsonObject {
                put("group_id", 0)
                if (messageKey != null) put("msg_key", messageKey.toString())
            }.toString(),
            "extra" to buildJsonObject { put("msg_keys", buildJsonArray {}) }.toString(),
        ))
        check(response.code == 0) { response.message.ifBlank { "举报提交失败" } }
    }
}
