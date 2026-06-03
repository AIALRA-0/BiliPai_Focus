package com.android.purebilibili.core.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTrackingPolicyTest {

    @Test
    fun normalizeAnalyticsDedupeToken_collapsesWhitespaceAndLimitsLength() {
        val normalized = normalizeAnalyticsDedupeToken("  screen   home  ")
        assertEquals("screen_home", normalized)

        val longInput = "x".repeat(120)
        assertEquals(80, normalizeAnalyticsDedupeToken(longInput).length)
    }

    @Test
    fun shouldSkipAnalyticsEvent_respectsMinInterval() {
        assertFalse(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = null,
                nowElapsedMs = 1000L,
                minIntervalMs = 800L
            )
        )
        assertTrue(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = 1500L,
                nowElapsedMs = 2000L,
                minIntervalMs = 800L
            )
        )
        assertFalse(
            shouldSkipAnalyticsEvent(
                lastLoggedAtMs = 1000L,
                nowElapsedMs = 2000L,
                minIntervalMs = 800L
            )
        )
    }

    @Test
    fun sensitiveAnalyticsKeysAndUserIdAreBlocked() {
        assertTrue(isSensitiveAnalyticsParamKey("video_id"))
        assertTrue(isSensitiveAnalyticsParamKey("room_id"))
        assertTrue(isSensitiveAnalyticsParamKey("season_id"))
        assertTrue(isSensitiveAnalyticsParamKey("episode_id"))
        assertTrue(isSensitiveAnalyticsParamKey("target_user_id"))
        assertFalse(isSensitiveAnalyticsParamKey("error_type"))
        assertFalse(isSensitiveAnalyticsParamKey("progress_percent"))

        assertNull(resolveAnalyticsUserId(mid = 123456L))
        assertNull(resolveAnalyticsUserId(mid = null))
    }

    @Test
    fun dailyActiveTracking_usesAnonymousDailyHeartbeatAndLocalDateDedupe() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/util/AnalyticsHelper.kt")

        assertTrue(source.contains("fun logDailyActive(source: String)"))
        assertTrue(source.contains("if (!isEnabled) return"))
        assertTrue(source.contains("\"daily_active\""))
        assertTrue(source.contains("\"last_daily_active_date\""))
        assertTrue(source.contains("LocalDate.now().toString()"))
        assertTrue(source.contains("if (lastLoggedDate == todayLocalDate) return"))
        assertTrue(source.contains("param(\"app_version\", BuildConfig.VERSION_NAME)"))
        assertTrue(source.contains("param(\"build_type\", BuildConfig.BUILD_TYPE)"))
        assertTrue(source.contains("param(\"local_date\", todayLocalDate)"))
        assertTrue(source.contains("param(\"source\", source)"))
        assertFalse(source.contains("param(\"mid\""))
        assertFalse(source.contains("setUserId(resolveAnalyticsUserId(mid))"))
    }

    @Test
    fun applicationLogsDailyActiveOnStartAndForeground() {
        val appSource = loadSource("app/src/main/java/com/android/purebilibili/app/PureApplication.kt")
        val helperSource = loadSource("app/src/main/java/com/android/purebilibili/core/util/AnalyticsHelper.kt")

        assertTrue(appSource.contains("AnalyticsHelper.logDailyActive(source = \"app_start\")"))
        assertTrue(helperSource.contains("logDailyActive(source = \"foreground\")"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
