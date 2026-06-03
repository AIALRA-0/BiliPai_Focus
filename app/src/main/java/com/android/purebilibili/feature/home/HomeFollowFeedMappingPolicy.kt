package com.android.purebilibili.feature.home

internal fun resolveDynamicArchiveAid(
    archiveAid: String,
    fallbackId: Long
): Long {
    return archiveAid.toLongOrNull()?.takeIf { it > 0 } ?: fallbackId
}

internal fun shouldIncludeHomeFollowDynamicInVideoFeed(
    archiveBvid: String
): Boolean {
    return archiveBvid.isNotBlank()
}
