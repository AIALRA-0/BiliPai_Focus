package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.repository.AiSummaryFetchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiSummaryRetryPolicyTest {

    @Test
    fun queuedSummaryUsesProgressiveRetryBackoff() {
        assertEquals(3_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 0))
        assertEquals(8_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 1))
        assertEquals(15_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 2))
        assertEquals(30_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 3))
        assertEquals(60_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 4))
        assertEquals(120_000L, resolveAiSummaryRetryDelayMs(queuedRetryCount = 5))
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
                queuedRetryCount = 6
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
            3_000L,
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
            30_000L,
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
