package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import java.security.MessageDigest

fun videoMetadataAccountScope(): String = buildVideoRequestAccountScope(
    listOf(TokenManager.sessDataCache, TokenManager.midCache?.toString())
)

/**
 * 播放账号作用域（缓存键的一部分）。
 *
 * 每次调用都要 `MessageDigest.getInstance("SHA-256")` + 多段字符串拼接，而它被用作
 * `PlayUrlCache` 的默认参数、在 `@Synchronized` 块内求值，因此必须记忆化。
 *
 * 记忆化键 = 参与哈希的 5 个原始字段值。只有这些字段真的变化才重算哈希：
 * - 与 [buildVideoRequestAccountScope] 的输入完全同源，**不改变任何哈希结果**；
 * - 账号切换 / VIP 状态刷新会改变字段，键自然不同，不会返回过期作用域。
 */
private val playbackScopeLock = Any()
private var cachedPlaybackScopeInputs: List<String>? = null
private var cachedPlaybackScope: String? = null

private fun resolvePlaybackScopeInputs(): List<String> {
    val account = NetworkModule.playbackAccount()
    return listOf(
        account?.sessData ?: TokenManager.sessDataCache,
        account?.mid?.toString() ?: TokenManager.midCache?.toString(),
        if (account != null) account.accessToken else TokenManager.accessTokenCache,
        account?.accessTokenPlatform ?: TokenManager.accessTokenPlatformCache,
        (account?.isVip ?: TokenManager.isVipCache).toString()
    ).map { it.orEmpty() }
}

fun videoPlaybackAccountScope(): String {
    val inputs = resolvePlaybackScopeInputs()
    synchronized(playbackScopeLock) {
        if (cachedPlaybackScopeInputs == inputs) {
            cachedPlaybackScope?.let { return it }
        }
        val scope = buildVideoRequestAccountScope(inputs)
        cachedPlaybackScopeInputs = inputs
        cachedPlaybackScope = scope
        return scope
    }
}

internal fun buildVideoRequestAccountScope(parts: List<String?>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    parts.forEach { part ->
        val bytes = part.orEmpty().toByteArray(Charsets.UTF_8)
        digest.update(bytes.size.toString().toByteArray(Charsets.UTF_8))
        digest.update(':'.code.toByte())
        digest.update(bytes)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

/** 推荐用途独立进入相关视频缓存键；不可用的指定账号不会命中主账号缓存。 */
fun recommendationAccountScope(): String {
    val context = NetworkModule.appContext
    val purpose = com.android.purebilibili.core.store.AccountPurpose.RECOMMENDATION
    val selected = context?.let { com.android.purebilibili.core.store.AccountSessionStore.getPurposeAccountMid(it, purpose) }
    val account = context?.let { com.android.purebilibili.core.store.AccountSessionStore.getPurposeAccount(it, purpose) }
    return buildVideoRequestAccountScope(if (selected == null) {
        listOf(TokenManager.sessDataCache, TokenManager.midCache?.toString())
    } else {
        listOf(selected.toString(), account?.sessData, account?.accessToken)
    })
}
