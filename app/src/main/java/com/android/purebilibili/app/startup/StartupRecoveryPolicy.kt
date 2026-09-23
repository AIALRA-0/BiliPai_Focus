package com.android.purebilibili.app

internal const val STARTUP_RECOVERY_THRESHOLD = 2
internal const val STARTUP_RECOVERY_WINDOW_MS = 5 * 60_000L
internal const val STARTUP_STABLE_FOREGROUND_MS = 2_500L

internal data class StartupRecoveryDecision(val failedAttempts: Int, val recover: Boolean)

/** An interrupted foreground startup is evidence of failure, not proof of a Java crash. */
internal fun resolveStartupRecovery(
    previousAttempts: Int,
    previousAttemptPending: Boolean,
    recoveryLatched: Boolean,
    sameBuild: Boolean,
    previousAttemptAt: Long,
    now: Long,
): StartupRecoveryDecision {
    if (!sameBuild) return StartupRecoveryDecision(0, false)
    if (recoveryLatched) return StartupRecoveryDecision(STARTUP_RECOVERY_THRESHOLD, true)
    val recent = now >= previousAttemptAt && now - previousAttemptAt <= STARTUP_RECOVERY_WINDOW_MS
    val failures = if (previousAttemptPending && recent) {
        previousAttempts.coerceIn(0, STARTUP_RECOVERY_THRESHOLD)
    } else 0
    return StartupRecoveryDecision(failures, failures >= STARTUP_RECOVERY_THRESHOLD)
}
