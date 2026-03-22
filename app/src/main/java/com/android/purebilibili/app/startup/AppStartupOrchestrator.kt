package com.android.purebilibili.app

import android.os.Build
import android.os.Handler
import android.os.Looper

internal class AppStartupOrchestrator(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val deferredDelayMs: Long = deferredNonCriticalStartupDelayMs(),
    private val dex2OatDelayMs: Long = dex2OatProfileInstallDelayMs(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    fun startupTasks(): List<AppStartupTask> {
        return defaultAppStartupTasks(
            sdkInt = sdkInt,
            deferredDelayMs = deferredDelayMs,
            dex2OatDelayMs = dex2OatDelayMs
        )
    }

    fun runImmediate(taskRunner: (AppStartupTask) -> Unit) {
        startupTasks()
            .filter { it.phase == StartupPhase.BEFORE_FIRST_INTERACTIVE }
            .forEach(taskRunner)
    }

    fun scheduleDeferred(taskRunner: (AppStartupTask) -> Unit) {
        val deferredTasks = startupTasks()
            .filter { it.phase == StartupPhase.AFTER_FIRST_INTERACTIVE }

        val idleTasks = deferredTasks.filter { it.thread == StartupThread.MAIN_IDLE }
        if (idleTasks.isNotEmpty()) {
            Looper.myQueue().addIdleHandler {
                idleTasks.forEach(taskRunner)
                false
            }
        }

        deferredTasks
            .filter { it.thread == StartupThread.MAIN_DELAYED }
            .forEach { task ->
                mainHandler.postDelayed({ taskRunner(task) }, task.delayMs)
            }
    }
}
