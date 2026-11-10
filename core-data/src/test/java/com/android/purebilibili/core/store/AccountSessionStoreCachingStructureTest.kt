package com.android.purebilibili.core.store

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 账号读取路径的缓存不变量。
 *
 * 背景：`getAccounts` / `getPlaybackAccount` 在播放取流、画质判定、账号作用域哈希上被高频
 * 调用，而原实现每次都要「SP 读 + AES/GCM 解密 + JSON 反序列化 + 排序」。配置独立播放
 * 账号后这条链路从「全部短路」变成「每次解密」，是「正式版（双账号）比 dev（单账号）
 * 明显更卡」的主要来源。
 *
 * 依赖 Android SharedPreferences / Keystore，无法纯单测；此结构用例锁定：
 * ① 缓存以密文原文为有效期键（账号写入必然产生新密文 → 天然失效，不会读到过期账号）；
 * ② 解密临时失败（密文非空却解析为空）时不写入缓存，允许下次重试，不把失败固化。
 */
class AccountSessionStoreCachingStructureTest {

    @Test
    fun accountReadsAreMemoizedOnTheCiphertext() {
        val source = storeSource()

        assertTrue(source.contains("private var cachedAccountsRaw: String? = null"))
        assertTrue(source.contains("private var cachedAccounts: List<StoredAccountSession> = emptyList()"))
        // 命中判定基于「密文原文未变」，因此账号写入后自然失效，无需手工清理。
        assertTrue(source.contains("if (cachedRaw == raw) return cachedAccounts"))

        // 播放账号解析同样记忆化（键 = 密文 + playback mid）。
        assertTrue(source.contains("private var cachedPlaybackRaw: String? = null"))
        assertTrue(source.contains("private var cachedPlaybackMid: Long? = null"))
        assertTrue(source.contains("if (cachedPlaybackRaw == raw && cachedPlaybackMid == mid)"))
    }

    @Test
    fun transientDecryptFailureIsNotCached() {
        val source = storeSource()

        assertTrue(source.contains("private fun canCacheDecodedAccounts("))
        assertTrue(source.contains("raw.isBlank() || decoded.isNotEmpty()"))
        // 写入缓存前必须过这道闸；否则一次 Keystore 抖动会固化「无账号」直到下次写入。
        assertTrue(source.contains("if (canCacheDecodedAccounts(raw, decoded))"))
        val playbackBlock = source.substringAfter("fun getPlaybackAccount(")
            .substringBefore("/** Selects an already-verified local account")
        assertTrue(playbackBlock.contains("if (canCacheDecodedAccounts(raw, accounts))"))
    }

    @Test
    fun apiClientResolvesTheAccountOutsideTheClientLock() {
        val source = listOf(
            File("core-data/src/main/java/com/android/purebilibili/core/network/ApiClient.kt"),
            File("src/main/java/com/android/purebilibili/core/network/ApiClient.kt"),
        ).first { it.exists() }.readText()

        // 账号解析（SP 读 + 可能的解密）必须在锁外；锁内只做键比较与客户端缓存读写。
        assertTrue(source.contains("private fun resolvePlaybackAccountClientKey()"))
        assertFalse(
            source.contains("@Synchronized\n    fun playbackApi()"),
            "playbackApi must not hold the global lock while resolving the account",
        )
        assertFalse(
            source.contains("@Synchronized\n    fun playbackBangumiApi()"),
            "playbackBangumiApi must not hold the global lock while resolving the account",
        )
        assertTrue(source.contains("private fun ensurePlaybackAccountApi("))
        assertTrue(source.contains("private fun buildPlaybackAccountClientsLocked("))
    }

    private fun storeSource(): String = listOf(
        File("core-data/src/main/java/com/android/purebilibili/core/store/AccountSessionStore.kt"),
        File("src/main/java/com/android/purebilibili/core/store/AccountSessionStore.kt"),
    ).first { it.exists() }.readText()
}
