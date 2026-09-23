package com.android.purebilibili.data.repository

import com.android.purebilibili.core.store.SettingsManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePreloadPolicyTest {

    @Test
    fun startsHomePreloadOnlyWhenNoCacheAndNoTaskExist() {
        assertTrue(
            shouldStartHomePreload(
                hasPreloadedData = false,
                hasActivePreloadTask = false
            )
        )
        assertFalse(
            shouldStartHomePreload(
                hasPreloadedData = true,
                hasActivePreloadTask = false
            )
        )
        assertFalse(
            shouldStartHomePreload(
                hasPreloadedData = false,
                hasActivePreloadTask = true
            )
        )
    }

    @Test
    fun primesBuvidOnlyForMobileFeedApi() {
        assertFalse(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.WEB))
        assertTrue(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.MOBILE))
    }

    @Test
    fun videoRepositoryInitSkipsSpiWarmupForWebAndUsesSharedWbiManager() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/data/repository/VideoRepository.kt"),
            java.io.File("src/main/java/com/android/purebilibili/data/repository/VideoRepository.kt")
        ).first { it.exists() }.readText()
        val initSource = source
            .substringAfter("fun init(context: android.content.Context)")
            .substringBefore("private suspend fun fetchPlayUrlWithAccessToken")

        assertTrue(initSource.contains("shouldPrimeBuvidForHomePreload(feedApiType)"))
        assertTrue(initSource.contains("WbiKeyManager.getWbiKeys()"))
        assertFalse(initSource.contains("runCatching { getWbiKeys() }"))
        assertFalse(initSource.contains("runCatching { ensureBuvid3FromSpi() }\n            runCatching { getWbiKeys() }"))
    }

    @Test
    fun reusesInFlightPreloadOnlyForInitialHomeRequest() {
        assertTrue(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 1,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = false,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = true
            )
        )
    }

    @Test
    fun reportsHomeDataReadyAfterPreloadCompletesEvenIfCacheWasConsumed() {
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = true,
                hasPreloadedData = false
            )
        )
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = true
            )
        )
        assertFalse(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = false
            )
        )
    }
}
