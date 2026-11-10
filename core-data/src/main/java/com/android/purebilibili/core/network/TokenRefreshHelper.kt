package com.android.purebilibili.core.network

import android.content.Context
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.network.CoreDataLog as Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Token 刷新助手
 * 用于刷新 TV 端登录的 access_token
 */
object TokenRefreshHelper {
    private const val TAG = "TokenRefreshHelper"
    private val mutex = Mutex()
    
    /**
     * 尝试刷新 Token
     * 如果刷新成功，会自动更新 TokenManager 中的缓存
     * 
     * @return true if refresh success, false otherwise
     */
    suspend fun refresh(context: Context): Boolean = mutex.withLock {
        try {
            if (TokenManager.accessTokenPlatformCache != TokenManager.ACCESS_TOKEN_PLATFORM_TV) {
                Logger.d(TAG, "Refresh is unavailable for this token platform")
                return false
            }
            val expectedSession = AccountCookieSession.main()
            val refreshToken = TokenManager.refreshTokenCache
            if (refreshToken.isNullOrEmpty()) {
                Logger.d(TAG, "Refresh failed: No refresh_token available")
                return false
            }
            
            Logger.d(TAG, "Starting token refresh...")
            
            val params = mapOf(
                "access_key" to (TokenManager.accessTokenCache ?: ""),
                "refresh_token" to refreshToken,
                "appkey" to AppSignUtils.TV_APP_KEY,
                "ts" to AppSignUtils.getTimestamp().toString()
            )
            
            val signedParams = AppSignUtils.signForTvLogin(params)
            
            val response = NetworkModule.passportApi.refreshToken(signedParams)
            
            if (response.code == 0 && response.data != null) {
                val data = response.data
                Logger.d(TAG, "Token refresh success!")
                
                if (TokenManager.sessDataCache.orEmpty() != expectedSession.sessData) return false
                val cookies = AccountCookieStore.read(context, expectedSession) +
                    data.cookieInfo?.cookies.orEmpty().associate { it.name to it.value }
                com.android.purebilibili.data.repository.SessionRepository.save(
                    context = context,
                    sessData = cookies["SESSDATA"].orEmpty(),
                    csrf = cookies["bili_jct"].orEmpty(),
                    buvid3 = cookies["buvid3"].orEmpty(),
                    mid = cookies["DedeUserID"]?.toLongOrNull() ?: TokenManager.midCache ?: 0L,
                    accessToken = data.accessToken, refreshToken = data.refreshToken, cookies = cookies,
                )
                com.android.purebilibili.core.store.AccountSessionStore.upsertCurrentAccount(context)

                return true
            } else {
                Logger.e(TAG, "Token refresh failed: code=${response.code}, msg=${response.message}")
                return false
            }
        } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            Logger.e(TAG, "Token refresh exception", e)
            return false
        }
    }
}
