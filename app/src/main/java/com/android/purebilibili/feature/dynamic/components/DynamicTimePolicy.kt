package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.FormatUtils

internal fun resolveDynamicAuthorTimeText(
    pubTime: String,
    pubTs: Long,
    nowMs: Long = System.currentTimeMillis()
): String {
    return if (pubTs > 0L) {
        FormatUtils.formatPublishTime(timestampSeconds = pubTs, nowMs = nowMs)
    } else {
        pubTime
    }
}
