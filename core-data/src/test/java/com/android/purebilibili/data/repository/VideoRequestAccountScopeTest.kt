package com.android.purebilibili.data.repository

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoRequestAccountScopeTest {
    @Test
    fun accountCredentialsAreNotStoredInCacheKeys() {
        val scope = buildVideoRequestAccountScope(listOf("private-session", "123", "true"))
        assertEquals(64, scope.length)
        assertFalse(scope.contains("private-session"))
    }

    @Test
    fun accountAndPermissionChangesProduceDifferentScopes() {
        val base = buildVideoRequestAccountScope(listOf("session-one", "123", "false"))
        assertNotEquals(base, buildVideoRequestAccountScope(listOf("session-two", "456", "false")))
        assertNotEquals(base, buildVideoRequestAccountScope(listOf("session-one", "123", "true")))
        assertEquals(base, buildVideoRequestAccountScope(listOf("session-one", "123", "false")))
    }

    @Test
    fun boundariesBetweenCredentialsAreUnambiguous() {
        assertNotEquals(
            buildVideoRequestAccountScope(listOf("ab", "c")),
            buildVideoRequestAccountScope(listOf("a", "bc"))
        )
    }

    /**
     * `videoPlaybackAccountScope()` 是 `PlayUrlCache` 的默认参数，在 `@Synchronized` 块内求值，
     * 且每次都要 `MessageDigest.getInstance("SHA-256")` + 多段拼接。必须记忆化，否则双账号
     * 场景下每次取流都重复付出哈希成本。
     *
     * 依赖 `NetworkModule` / `TokenManager` 运行时状态，无法纯单测；此结构用例锁定
     * 「记忆化键与哈希输入同源」这一不变量，避免日后改回每次重算或漏用缓存键。
     */
    @Test
    fun playbackScopeIsMemoizedOnTheSameInputsAsTheHash() {
        val source = listOf(
            File("core-data/src/main/java/com/android/purebilibili/data/repository/VideoRequestAccountScope.kt"),
            File("src/main/java/com/android/purebilibili/data/repository/VideoRequestAccountScope.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("private fun resolvePlaybackScopeInputs()"))
        assertTrue(source.contains("cachedPlaybackScopeInputs"))
        assertTrue(source.contains("if (cachedPlaybackScopeInputs == inputs)"))
        // 求值入口仍然复用同一份 inputs 交给哈希函数，保证结果不因缓存而改变。
        assertTrue(source.contains("val scope = buildVideoRequestAccountScope(inputs)"))
        // 参与哈希的 5 个字段必须仍在 inputs 里（sessData/mid/accessToken/platform/isVip）。
        val inputs = source.substringAfter("private fun resolvePlaybackScopeInputs()")
            .substringBefore("fun videoPlaybackAccountScope()")
        assertTrue(inputs.contains("sessData"))
        assertTrue(inputs.contains("mid"))
        assertTrue(inputs.contains("accessToken"))
        assertTrue(inputs.contains("accessTokenPlatform"))
        assertTrue(inputs.contains("isVip"))
    }
}
