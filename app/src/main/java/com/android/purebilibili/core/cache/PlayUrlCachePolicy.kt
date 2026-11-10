package com.android.purebilibili.core.cache

import java.net.URI

internal fun resolvePlayUrlCacheExpiresAt(
    timestampMs: Long,
    maxAgeMs: Long,
    urls: List<String>,
    safetyMarginMs: Long = 30_000L
): Long {
    val deadlineMs = urls.mapNotNull { url ->
        runCatching {
            URI(url).rawQuery?.split('&')?.firstNotNullOfOrNull { parameter ->
                if (parameter.substringBefore('=') != "deadline") return@firstNotNullOfOrNull null
                parameter.substringAfter('=', "").toLongOrNull()
                    ?.takeIf { it in 1L..(Long.MAX_VALUE / 1000L) }
                    ?.times(1000L)
            }
        }.getOrNull()
    }.minOrNull()
    val maxExpiresAtMs = timestampMs + maxAgeMs
    return if (deadlineMs == null) maxExpiresAtMs
    else minOf(maxExpiresAtMs, deadlineMs - safetyMarginMs)
}
