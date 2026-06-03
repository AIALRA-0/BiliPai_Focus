package com.android.purebilibili.app

import android.content.ComponentCallbacks2
import android.os.Build
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter

internal object PureApplicationRuntimeConfig {
    const val TAG: String = "PureApplication"

    fun shouldBlockStartupForHomeVisualDefaultsMigration(): Boolean = false

    fun shouldDeferPlaylistRestoreAtStartup(): Boolean = true

    fun shouldDeferTelemetryInitAtStartup(): Boolean = true

    fun deferredNonCriticalStartupDelayMs(): Long = 900L

    fun shouldRequestDex2OatProfileInstall(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.N

    fun dex2OatProfileInstallDelayMs(): Long = 2_500L

    fun resolveImageMemoryCachePercent(): Double = 0.10

    fun resolveImageMemoryCacheTrimLevel(level: Int): Int? {
        return when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> level
            else -> null
        }
    }

    fun shouldClearImageMemoryCacheOnTrimLevel(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
            level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
    }

    fun createTelemetryBackgroundStateListener(): BackgroundManager.BackgroundStateListener =
        TelemetryBackgroundStateListener

    internal object TelemetryBackgroundStateListener : BackgroundManager.BackgroundStateListener {
        override fun onEnterBackground() {
            AnalyticsHelper.onAppBackground()
            CrashReporter.setAppForegroundState(false)
        }

        override fun onEnterForeground() {
            AnalyticsHelper.onAppForeground()
            CrashReporter.setAppForegroundState(true)
        }
    }
}
