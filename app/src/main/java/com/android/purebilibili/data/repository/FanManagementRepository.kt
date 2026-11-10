package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

internal data class FanRemoved(val identity: AccountSessionIdentity, val mid: Long)

internal fun fanRemovalFields(ownerMid: Long, targetMid: Long, csrf: String): Map<String, String> {
    require(ownerMid > 0L && targetMid > 0L && ownerMid != targetMid) { "无法移除该粉丝" }
    require(csrf.isNotBlank()) { "请重新登录后操作" }
    return mapOf("fid" to targetMid.toString(), "act" to "7", "re_src" to "11", "csrf" to csrf)
}

internal object FanManagementRepository {
    // 列表与空间同时订阅成功事件；新打开的页面以服务端结果为准。
    private val _removed = MutableSharedFlow<FanRemoved>()
    val removed = _removed.asSharedFlow()
    private val removing = mutableSetOf<Pair<AccountSessionIdentity, Long>>()

    private fun credentials(identity: AccountSessionIdentity): Pair<String, String> = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value && identity.mid == TokenManager.midCache) { "账号已切换，请重新打开页面" }
        require((identity.mid ?: 0L) > 0L) { "请先登录" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "请重新登录" }
        val csrf = TokenManager.csrfCache.orEmpty()
        "SESSDATA=$session; bili_jct=$csrf; DedeUserID=${identity.mid}" to csrf
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try { Result.success(block()) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Result.failure(error) }
    }

    suspend fun isFan(identity: AccountSessionIdentity, mid: Long): Result<Boolean> = request {
        val (cookie, _) = credentials(identity)
        val response = NetworkModule.api.getFanRelationship(mid, cookie)
        check(response.code == 0) { response.message.ifBlank { "读取粉丝关系失败（${response.code}）" } }
        requireNotNull(response.data?.reverseRelation) { "粉丝关系暂不可用" }.isFollowing
    }

    suspend fun remove(identity: AccountSessionIdentity, mid: Long): Result<Unit> {
        val key = identity to mid
        if (!synchronized(removing) { removing.add(key) }) {
            return Result.failure(IllegalStateException("正在移除该粉丝，请稍候"))
        }
        try {
            val result = request {
                val (cookie, csrf) = credentials(identity)
                val fields = fanRemovalFields(requireNotNull(identity.mid), mid, csrf)
                val response = NetworkModule.api.removeAccountFan(fields, cookie)
                check(response.code == 0) { response.message.ifBlank { "移除粉丝失败（${response.code}）" } }
            }
            if (result.isSuccess) _removed.emit(FanRemoved(identity, mid))
            return result
        } finally {
            synchronized(removing) { removing.remove(key) }
        }
    }
}
