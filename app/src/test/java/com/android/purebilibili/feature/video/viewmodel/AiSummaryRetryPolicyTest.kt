package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.repository.AiSummaryFetchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiSummaryRetryPolicyTest {

    @Test
    fun queuedSummaryUsesProgressiveRetryBackoff() {
        assertEquals(2_500L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 0))
        assertEquals(5_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 1))
        assertEquals(10_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 2))
        assertEquals(20_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 3))
    }

    @Test
    fun queuedSummaryStopsAutoRetryAfterConfiguredBudget() {
        assertTrue(
            shouldContinueAiSummaryAutoRetry(
                status = AiSummaryFetchStatus.QUEUED,
                queuedRetryCount = 0
            )
        )
        assertTrue(
            shouldContinueAiSummaryAutoRetry(
                status = AiSummaryFetchStatus.QUEUED,
                queuedRetryCount = 3
            )
        )
        assertFalse(
            shouldContinueAiSummaryAutoRetry(
                status = AiSummaryFetchStatus.QUEUED,
                queuedRetryCount = 4
            )
        )
        assertFalse(
            shouldContinueAiSummaryAutoRetry(
                status = AiSummaryFetchStatus.NO_SUMMARY,
                queuedRetryCount = 0
            )
        )
    }

    @Test
    fun queuedSummaryRetryDelayShouldBackOffFurtherInBackground() {
        assertEquals(
            2_500L,
            resolveAiSummaryRetryDelayMs(
                queuedRetryCount = 0,
                isInBackground = false
            )
        )
        assertEquals(
            15_000L,
            resolveAiSummaryRetryDelayMs(
                queuedRetryCount = 0,
                isInBackground = true
            )
        )
        assertEquals(
            20_000L,
            resolveAiSummaryRetryDelayMs(
                queuedRetryCount = 3,
                isInBackground = true
            )
        )
    }

    @Test
    fun retryableRequestFailureUsesSmallAutoRetryBudget() {
        assertTrue(
            shouldRetryAiSummaryRequestFailure(
                status = AiSummaryFetchStatus.RETRYABLE_FAILURE,
                requestRetryCount = 0
            )
        )
        assertTrue(
            shouldRetryAiSummaryRequestFailure(
                status = AiSummaryFetchStatus.RETRYABLE_FAILURE,
                requestRetryCount = 1
            )
        )
        assertFalse(
            shouldRetryAiSummaryRequestFailure(
                status = AiSummaryFetchStatus.RETRYABLE_FAILURE,
                requestRetryCount = 2
            )
        )
        assertFalse(
            shouldRetryAiSummaryRequestFailure(
                status = AiSummaryFetchStatus.FAILURE,
                requestRetryCount = 0
            )
        )
    }
}
