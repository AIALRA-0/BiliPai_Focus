package com.android.purebilibili.feature.profile

internal data class ProfileLevelProgress(
    val progress: Float,
    val currentExp: Int,
    val nextExp: Int,
)

internal fun resolveProfileLevelProgress(
    currentMinExp: Int,
    currentExp: Int,
    nextExp: Int,
): ProfileLevelProgress {
    val safeCurrent = currentExp.coerceAtLeast(currentMinExp)
    val safeNext = nextExp.coerceAtLeast(safeCurrent)
    val range = (safeNext - currentMinExp).coerceAtLeast(1)
    return ProfileLevelProgress(
        progress = ((safeCurrent - currentMinExp).toFloat() / range).coerceIn(0f, 1f),
        currentExp = safeCurrent,
        nextExp = safeNext,
    )
}

internal enum class ProfileDashboardShortcut {
    DOWNLOAD,
    HISTORY,
    SUBSCRIPTION,
    WATCH_LATER,
}

internal fun defaultProfileDashboardShortcuts(): List<ProfileDashboardShortcut> = listOf(
    ProfileDashboardShortcut.DOWNLOAD,
    ProfileDashboardShortcut.HISTORY,
    ProfileDashboardShortcut.SUBSCRIPTION,
    ProfileDashboardShortcut.WATCH_LATER,
)
