package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.AccountLoginDevice
import com.android.purebilibili.data.model.response.AccountLoginRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AccountLoginActivityRepository {
    private class Credentials(
        val cookie: String,
        val csrf: String,
        val accessKey: String,
        val platform: String,
        val buvid: String,
    )

    private fun credentials(identity: AccountSessionIdentity): Credentials = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value) { "账号已切换，请重新加载" }
        val mid = requireNotNull(identity.mid?.takeIf { it > 0L }) { "请先登录" }
        check(TokenManager.midCache == mid) { "账号正在切换，请稍后重试" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "登录状态已失效，请重新登录" }
        val csrf = TokenManager.csrfCache.orEmpty()
        Credentials(
            cookie = "SESSDATA=$session; bili_jct=$csrf; DedeUserID=$mid",
            csrf = csrf,
            accessKey = TokenManager.accessTokenCache.orEmpty(),
            platform = TokenManager.accessTokenPlatformCache,
            buvid = TokenManager.buvid3Cache.orEmpty(),
        )
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun devices(identity: AccountSessionIdentity): Result<List<AccountLoginDevice>> = request {
        val auth = credentials(identity)
        val params = loginDeviceRequestParams(auth.accessKey, auth.platform, auth.csrf, auth.buvid, System.currentTimeMillis() / 1000L)
        val response = NetworkModule.api.getAccountLoginDevices(params, auth.cookie)
        check(response.code == 0) { response.message.ifBlank { "加载登录设备失败（${response.code}）" } }
        requireNotNull(response.data?.devices) { "登录设备数据不完整，请重试" }
    }

    suspend fun records(identity: AccountSessionIdentity): Result<List<AccountLoginRecord>> = request {
        val auth = credentials(identity)
        val response = NetworkModule.api.getAccountLoginRecords(auth.cookie)
        check(response.code == 0) { response.message.ifBlank { "加载登录记录失败（${response.code}）" } }
        requireNotNull(response.data?.list) { "登录记录数据不完整，请重试" }
    }
}
