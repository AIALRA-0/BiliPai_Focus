package com.android.purebilibili.core.store

/**
 * 播完策略双源对齐：
 * UI / Flow 读 DataStore，播完回调走 Sync（历史读 SharedPreferences）。
 * 若两边脱节，会出现「界面已是顺序播放，实际仍按单个循环 seekTo(0)」.
 */
internal fun resolvePlaybackCompletionBehaviorSyncSource(
    memoryCacheValue: Int?,
    sharedPreferencesValue: Int?,
    defaultValue: Int = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC.value,
): PlaybackCompletionBehavior {
    val raw = memoryCacheValue ?: sharedPreferencesValue ?: defaultValue
    return PlaybackCompletionBehavior.fromValue(raw)
}

internal fun shouldHealPlaybackCompletionSharedPreferences(
    dataStoreValue: Int,
    sharedPreferencesValue: Int?,
): Boolean {
    return sharedPreferencesValue != dataStoreValue
}
