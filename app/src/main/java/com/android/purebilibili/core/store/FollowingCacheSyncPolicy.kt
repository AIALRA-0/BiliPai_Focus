package com.android.purebilibili.core.store

const val FOLLOWING_CACHE_REFRESH_TTL_MS: Long = 5 * 60 * 1000L

fun shouldReloadFollowingCacheSnapshot(
    nowMs: Long,
    lastLoadMs: Long,
    cachedUsersCount: Int = 0,
    preferredUserCount: Int = 0,
    hasCompleteSnapshot: Boolean = false,
    ttlMs: Long = FOLLOWING_CACHE_REFRESH_TTL_MS
): Boolean {
    if (lastLoadMs <= 0L) return true
    if (
        !hasCompleteSnapshot &&
        preferredUserCount > 0 &&
        cachedUsersCount in 1 until preferredUserCount
    ) {
        return true
    }
    return nowMs - lastLoadMs >= ttlMs
}
