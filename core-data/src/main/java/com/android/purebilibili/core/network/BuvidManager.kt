package com.android.purebilibili.core.network

import com.android.purebilibili.core.store.StoredAccountSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 设备标识跟随账号 Cookie 保持稳定；每个会话在进程内至多激活一次。 */
object BuvidManager {
    private val mutex = Mutex()
    private val activatedSessions = mutableSetOf<String>()

    suspend fun ensureBuvid3(account: StoredAccountSession? = null): Unit = mutex.withLock {
        val session = account?.let(AccountCookieSession::from) ?: AccountCookieSession.main()
        val cookies = AccountCookieStore.read(NetworkModule.appContext, session)
        val key = session.storageKey + ":" + cookies["buvid3"].orEmpty()
        if (!activatedSessions.add(key)) return@withLock
        val api = if (account != null) NetworkModule.playbackApi() else NetworkModule.api
        try {
            val random = kotlin.random.Random
            val bytes = ByteArray(32) { random.nextInt(256).toByte() } +
                byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68) + ByteArray(4) { random.nextInt(256).toByte() }
            val pngEnd = java.util.Base64.getEncoder().encodeToString(bytes).takeLast(50)
            val payload = org.json.JSONObject().apply {
                put("3064", 1)
                put("39c8", "333.1387.fp.risk")
                put("3c43", org.json.JSONObject().apply {
                    put("adca", "Linux")
                    put("bfe9", pngEnd)
                })
            }.toString()
            api.activateSessionBuvid(mapOf("payload" to payload))
        } catch (cancelled: CancellationException) {
            activatedSessions.remove(key)
            throw cancelled
        } catch (error: Exception) {
            CoreDataLog.w("BuvidManager", "Device activation failed: ${error.message}")
        }
    }
}
