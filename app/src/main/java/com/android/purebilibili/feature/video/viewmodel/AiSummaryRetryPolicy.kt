package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.repository.AiSummaryFetchStatus

private val AI_SUMMARY_AUTO_RETRY_DELAYS_MS = listOf(
    3_000L,
    8_000L,
    15_000L,
    30_000L,
    60_000L,
    120_000L
)
private const val AI_SUMMARY_RETRYABLE_FAILURE_AUTO_RETRY_LIMIT = 2

internal fun resolveAiSummaryRetryDelayMs(queuedRetryCount: Int): Long {
    return resolveAiSummaryRetryDelayMs(
        queuedRetryCount = queuedRetryCount,
        isInBackground = false
    )
}

internal fun resolveAiSummaryRetryDelayMs(
    queuedRetryCount: Int,
    isInBackground: Boolean
): Long {
    val baseDelayMs = AI_SUMMARY_AUTO_RETRY_DELAYS_MS.getOrElse(queuedRetryCount) {
        AI_SUMMARY_AUTO_RETRY_DELAYS_MS.last()
    }
    return if (isInBackground) {
        maxOf(baseDelayMs, 15_000L)
    } else {
        baseDelayMs
    }
}

internal fun shouldContinueAiSummaryAutoRetry(
    status: AiSummaryFetchStatus,
    queuedRetryCount: Int
): Boolean {
    return status == AiSummaryFetchStatus.QUEUED &&
        queuedRetryCount < AI_SUMMARY_AUTO_RETRY_DELAYS_MS.size
}

internal fun shouldRetryAiSummaryRequestFailure(
    status: AiSummaryFetchStatus,
    requestRetryCount: Int
): Boolean {
    return status == AiSummaryFetchStatus.RETRYABLE_FAILURE &&
        requestRetryCount < AI_SUMMARY_RETRYABLE_FAILURE_AUTO_RETRY_LIMIT
}
