package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal object SpacePrivacyRepository {
    private suspend fun <T> request(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // 固定本次请求的账号凭证，避免切换账号后把旧页面的修改发给新账号。
    private fun credentials(identity: AccountSessionIdentity): Pair<String, String> = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value) { "账号已切换，请重新加载" }
        val mid = requireNotNull(identity.mid?.takeIf { it > 0L }) { "请先登录后管理空间公开范围" }
        check(TokenManager.midCache == mid) { "账号正在切换，请稍后重试" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "请重新登录" }
        val csrf = requireNotNull(TokenManager.csrfCache?.takeIf { it.isNotBlank() }) { "请重新登录" }
        check(identity == TokenManager.accountIdentity.value) { "账号已切换，请重新加载" }
        "SESSDATA=$session; bili_jct=$csrf; DedeUserID=$mid" to csrf
    }

    suspend fun load(identity: AccountSessionIdentity): Result<Map<String, Int>> = request {
        val (cookie, _) = credentials(identity)
        val response = NetworkModule.api.getSpacePrivacy(requireNotNull(identity.mid), cookie)
        check(response.code == 0) { response.message.ifBlank { "加载空间公开范围失败（${response.code}）" } }
        val privacy = requireNotNull(response.data?.privacy) { "空间公开范围数据为空" }
        privacy.mapNotNull { (key, value) ->
            (value as? JsonPrimitive)?.intOrNull?.takeIf { it == 0 || it == 1 }?.let { key to it }
        }.toMap()
    }

    suspend fun save(identity: AccountSessionIdentity, changes: Map<String, Int>): Result<Unit> = request {
        require(changes.isNotEmpty() && changes.values.all { it == 0 || it == 1 }) { "没有有效的修改" }
        val (cookie, csrf) = credentials(identity)
        val response = NetworkModule.api.updateSpacePrivacy(changes.mapValues { it.value.toString() } + ("csrf" to csrf), cookie)
        check(response.code == 0) { response.message.ifBlank { "保存空间公开范围失败（${response.code}）" } }
    }
}
