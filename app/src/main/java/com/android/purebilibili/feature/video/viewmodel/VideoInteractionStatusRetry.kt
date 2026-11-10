package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Retry initial status reads without inventing a false state after a failed request. */
internal suspend fun queryVideoInteractionStatusWithRetry(
    isCurrent: () -> Boolean,
    query: suspend () -> Result<Boolean>,
): Result<Boolean> {
    var result: Result<Boolean> = Result.failure(IllegalStateException("互动状态尚未确认"))
    repeat(3) { attempt ->
        if (attempt > 0) delay(1_000L * attempt)
        if (!isCurrent()) return Result.failure(IllegalStateException("互动查询已过期"))
        result = withTimeoutOrNull(8_000L) { query() }
            ?: Result.failure(IllegalStateException("互动状态查询超时"))
        (result.exceptionOrNull() as? CancellationException)?.let { throw it }
        if (result.isSuccess) return result
    }
    return result
}
