package com.android.purebilibili.app

import android.os.Build

internal enum class StartupPhase {
    BEFORE_FIRST_INTERACTIVE,
    AFTER_FIRST_INTERACTIVE
}

internal enum class StartupCriticality {
    REQUIRED,
    DEFERRED
}

internal enum class StartupThread {
    MAIN,
    MAIN_DELAYED,
    MAIN_IDLE
}

internal data class AppStartupTask(
    val id: String,
    val phase: StartupPhase,
    val criticality: StartupCriticality,
    val thread: StartupThread,
    val delayMs: Long = 0L
)

internal fun defaultAppStartupTasks(
    sdkInt: Int = Build.VERSION.SDK_INT,
    deferredDelayMs: Long = PureApplicationRuntimeConfig.deferredNonCriticalStartupDelayMs(),
    dex2OatDelayMs: Long = PureApplicationRuntimeConfig.dex2OatProfileInstallDelayMs()
): List<AppStartupTask> {
    val playlistPhase = if (PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup()) {
        StartupPhase.AFTER_FIRST_INTERACTIVE
    } else {
        StartupPhase.BEFORE_FIRST_INTERACTIVE
    }
    val playlistThread = if (PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup()) {
        StartupThread.MAIN_DELAYED
    } else {
        StartupThread.MAIN
    }
    val playlistCriticality = if (PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup()) {
        StartupCriticality.DEFERRED
    } else {
        StartupCriticality.REQUIRED
    }

    val telemetryPhase = if (PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup()) {
        StartupPhase.AFTER_FIRST_INTERACTIVE
    } else {
        StartupPhase.BEFORE_FIRST_INTERACTIVE
    }
    val telemetryThread = if (PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup()) {
        StartupThread.MAIN_DELAYED
    } else {
        StartupThread.MAIN
    }
    val telemetryCriticality = if (PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup()) {
        StartupCriticality.DEFERRED
    } else {
        StartupCriticality.REQUIRED
    }

    val tasks = mutableListOf(
        AppStartupTask(
            id = "network_module_init",
            phase = StartupPhase.BEFORE_FIRST_INTERACTIVE,
            criticality = StartupCriticality.REQUIRED,
            thread = StartupThread.MAIN
        ),
        AppStartupTask(
            id = "token_manager_init",
            phase = StartupPhase.BEFORE_FIRST_INTERACTIVE,
            criticality = StartupCriticality.REQUIRED,
            thread = StartupThread.MAIN
        ),
        AppStartupTask(
            id = "video_repository_init",
            phase = StartupPhase.BEFORE_FIRST_INTERACTIVE,
            criticality = StartupCriticality.REQUIRED,
            thread = StartupThread.MAIN
        ),
        AppStartupTask(
            id = "background_manager_init",
            phase = StartupPhase.BEFORE_FIRST_INTERACTIVE,
            criticality = StartupCriticality.REQUIRED,
            thread = StartupThread.MAIN
        ),
        AppStartupTask(
            id = "player_settings_cache_init",
            phase = StartupPhase.BEFORE_FIRST_INTERACTIVE,
            criticality = StartupCriticality.REQUIRED,
            thread = StartupThread.MAIN
        ),
        AppStartupTask(
            id = "notification_channel_init",
            phase = StartupPhase.AFTER_FIRST_INTERACTIVE,
            criticality = StartupCriticality.DEFERRED,
            thread = StartupThread.MAIN_IDLE
        ),
        AppStartupTask(
            id = "playlist_restore",
            phase = playlistPhase,
            criticality = playlistCriticality,
            thread = playlistThread,
            delayMs = if (playlistThread == StartupThread.MAIN_DELAYED) deferredDelayMs else 0L
        ),
        AppStartupTask(
            id = "telemetry_init",
            phase = telemetryPhase,
            criticality = telemetryCriticality,
            thread = telemetryThread,
            delayMs = if (telemetryThread == StartupThread.MAIN_DELAYED) deferredDelayMs else 0L
        ),
        AppStartupTask(
            id = "plugin_init",
            phase = StartupPhase.AFTER_FIRST_INTERACTIVE,
            criticality = StartupCriticality.DEFERRED,
            thread = StartupThread.MAIN_IDLE
        )
    )

    if (PureApplicationRuntimeConfig.shouldRequestDex2OatProfileInstall(sdkInt)) {
        tasks += AppStartupTask(
            id = "dex2oat_profile_install",
            phase = StartupPhase.AFTER_FIRST_INTERACTIVE,
            criticality = StartupCriticality.DEFERRED,
            thread = StartupThread.MAIN_DELAYED,
            delayMs = dex2OatDelayMs
        )
    }

    return tasks
}
