package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.DynamicDoVoteRequest
import com.android.purebilibili.data.model.response.DynamicVoteInfo
import com.android.purebilibili.data.model.response.toResolvedVoteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DynamicVoteRepository {
    suspend fun getVoteInfo(voteId: Long): Result<DynamicVoteInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val response = NetworkModule.dynamicApi.getVoteInfo(voteId)
            if (response.code != 0) {
                error(response.message.ifBlank { "投票信息加载失败" })
            }
            response.data?.toResolvedVoteInfo()
                ?: error("投票信息为空")
        }
    }

    suspend fun submitVote(
        voteId: Long,
        optionIndexes: List<Int>,
        dynamicId: String = "",
        anonymous: Boolean = false
    ): Result<DynamicVoteInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val csrf = TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) error("请先登录")
            val response = NetworkModule.dynamicApi.doVote(
                csrf = csrf,
                body = DynamicDoVoteRequest(
                    vote_id = voteId,
                    votes = optionIndexes,
                    voter_uid = TokenManager.midCache ?: 0L,
                    status = if (anonymous) 1 else 0,
                    dynamic_id = dynamicId.trim().toLongOrNull() ?: 0L,
                    csrf = csrf,
                    csrf_token = csrf
                )
            )
            if (response.code != 0) {
                error(response.message.ifBlank { "投票失败" })
            }
            response.data?.toResolvedVoteInfo()
                ?: error("投票结果为空")
        }
    }
}