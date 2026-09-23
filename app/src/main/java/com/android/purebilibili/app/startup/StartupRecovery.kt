package com.android.purebilibili.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.util.Logger

/** Only minimal platform APIs here: this path must survive failures in normal app setup. */
internal object StartupRecovery {
    private const val PREFS = "startup_recovery"
    private const val BUILD = "build"
    private const val ATTEMPTS = "attempts"
    private const val PENDING = "pending"
    private const val ATTEMPT_AT = "attempt_at"
    private const val RECOVERY = "recovery"
    private val buildKey: String
        get() = "${BuildConfig.VERSION_CODE}/${BuildConfig.BUILD_TYPE}/${BuildConfig.BUILD_COMMIT_SHA}"

    var isRecoveryMode: Boolean = false
        private set
    private var observingStartup = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var stableCallback: Runnable? = null

    fun beginLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val decision = resolveStartupRecovery(
            previousAttempts = prefs.getInt(ATTEMPTS, 0),
            previousAttemptPending = prefs.getBoolean(PENDING, false),
            recoveryLatched = prefs.getBoolean(RECOVERY, false),
            sameBuild = prefs.getString(BUILD, null) == buildKey,
            previousAttemptAt = prefs.getLong(ATTEMPT_AT, 0),
            now = now,
        )
        isRecoveryMode = decision.recover
        observingStartup = !decision.recover
        // Synchronous by design: a crash immediately after this point must leave a marker.
        prefs.edit()
            .putString(BUILD, buildKey)
            .putInt(ATTEMPTS, if (decision.recover) decision.failedAttempts else decision.failedAttempts + 1)
            .putBoolean(PENDING, !decision.recover)
            .putBoolean(RECOVERY, decision.recover)
            .putLong(ATTEMPT_AT, now)
            .commit()
        Logger.recordStartupStage(if (decision.recover) "recovery_entered" else "application_start")
    }

    fun prepareRetry(context: Context) {
        cancelStableCheck()
        // Keep the recovery latch on disk until success. Only this process leaves recovery;
        // an interrupted explicit retry must return to diagnostics even much later.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(RECOVERY, true)
            .putBoolean(PENDING, true)
            .putLong(ATTEMPT_AT, System.currentTimeMillis())
            .commit()
        isRecoveryMode = false
        observingStartup = true
        Logger.recordStartupStage("recovery_retry")
    }

    fun onMainResumed(activity: Activity) {
        if (!observingStartup || isRecoveryMode) return
        cancelStableCheck()
        Logger.recordStartupStage("main_resumed")
        // Run only after MainActivity has remained resumed long enough for deferred startup.
        stableCallback = Runnable {
            stableCallback = null
            if (!activity.isFinishing && !activity.isDestroyed) {
                completeObservation(activity, "startup_stable")
            }
        }.also { handler.postDelayed(it, STARTUP_STABLE_FOREGROUND_MS) }
    }

    fun onMainPaused(activity: Activity? = null) {
        cancelStableCheck()
        if (observingStartup && !isRecoveryMode && activity?.isFinishing == true) {
            completeObservation(activity, "startup_user_exit")
        }
    }

    fun onMainStopped(activity: Activity) {
        cancelStableCheck()
        if (observingStartup && !activity.isChangingConfigurations && !isRecoveryMode) {
            // Leaving the app normally must not count as another startup crash.
            completeObservation(activity, "startup_left_foreground")
        }
    }

    private fun completeObservation(context: Context, stage: String) {
        observingStartup = false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(PENDING, false)
            .putBoolean(RECOVERY, false)
            .putInt(ATTEMPTS, 0)
            .apply()
        Logger.recordStartupStage(stage)
    }

    private fun cancelStableCheck() {
        stableCallback?.let(handler::removeCallbacks)
        stableCallback = null
    }
}
