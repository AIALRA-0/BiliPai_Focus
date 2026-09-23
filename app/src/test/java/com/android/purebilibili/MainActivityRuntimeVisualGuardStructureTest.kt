package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MainActivityRuntimeVisualGuardStructureTest {

    @Test
    fun jankStatsFollowsActivityStartedLifecycleAndDiscardsInterruptedWindow() {
        val source = mainActivitySource()
        val onStartBlock = source.substringAfter("override fun onStart()").substringBefore("override fun onStop()")
        val onStopBlock = source.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        val onDestroyBlock = source.substringAfter("override fun onDestroy()").substringBefore("\n    }")

        assertTrue(onStartBlock.contains("JankStats.createAndTrack(window)"))
        assertTrue(onStartBlock.contains("activateSession(runtimeVisualGuardSession)"))
        assertTrue(onStartBlock.contains("SystemClock.uptimeMillis()"))
        assertTrue(onStopBlock.contains("discardActiveWindow(runtimeVisualGuardSession)"))
        assertTrue(onStopBlock.contains("isTrackingEnabled = false"))
        assertTrue(onDestroyBlock.contains("runtimeJankStats = null"))
    }

    @Test
    fun runtimeGuardUsesTheActualWindowWidthInBothActivityEntryPoints() {
        val mainSource = mainActivitySource()
        val videoSource = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt"),
        ).first { it.exists() }.readText()

        assertTrue(mainSource.contains("widthSizeClass = windowSizeClass.widthSizeClass"))
        assertTrue(mainSource.contains("currentWindowAdaptiveInfoV2()"))
        assertTrue(videoSource.contains("currentWindowAdaptiveInfoV2()"))
        assertTrue(videoSource.contains("ProvideRuntimeVisualGuard(widthSizeClass = windowWidthSizeClass)"))
    }

    @Test
    fun independentVideoActivityOwnsACompleteJankStatsLifecycle() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt"),
        ).first { it.exists() }.readText()
        val onStartBlock = source.substringAfter("override fun onStart()").substringBefore("override fun onStop()")
        val onStopBlock = source.substringAfter("override fun onStop()").substringBefore("override fun onDestroy()")
        val onDestroyBlock = source.substringAfter("override fun onDestroy()").substringBefore("override fun onConfigurationChanged")

        assertTrue(onStartBlock.contains("JankStats.createAndTrack(window)"))
        assertTrue(onStartBlock.contains("activateSession(runtimeVisualGuardSession)"))
        assertTrue(onStartBlock.contains("AppRuntimeVisualGuardTracker.onFrame("))
        assertTrue(onStartBlock.contains("SystemClock.uptimeMillis()"))
        assertTrue(onStopBlock.contains("discardActiveWindow(runtimeVisualGuardSession)"))
        assertTrue(onStopBlock.contains("isTrackingEnabled = false"))
        assertTrue(onDestroyBlock.contains("runtimeJankStats = null"))
    }

    private fun mainActivitySource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/MainActivity.kt"),
            File("src/main/java/com/android/purebilibili/MainActivity.kt"),
        ).first { it.exists() }.readText()
    }
}
