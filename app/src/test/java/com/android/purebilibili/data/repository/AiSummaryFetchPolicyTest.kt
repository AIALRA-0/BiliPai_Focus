package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.AiModelResult
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.data.model.response.AiSummaryResponse
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class AiSummaryFetchPolicyTest {

    @Test
    fun queuedSummaryIsRecognizedForDelayedRetry() {
        val diagnosis = diagnoseAiSummaryResponse(
            AiSummaryResponse(
                code = 0,
                data = AiSummaryData(
                    code = 1,
                    stid = "0"
                )
            )
        )

        assertEquals(AiSummaryFetchStatus.QUEUED, diagnosis.status)
        assertTrue(diagnosis.shouldRetryLater)
    }

    @Test
    fun unauthorizedResponseIsReportedExplicitly() {
        val diagnosis = diagnoseAiSummaryResponse(
            AiSummaryResponse(
                code = -101,
                message = "账号未登录"
            )
        )

        assertEquals(AiSummaryFetchStatus.UNAUTHORIZED, diagnosis.status)
        assertFalse(diagnosis.shouldRetryRequest)
    }

    @Test
    fun summaryWithoutSpeechIsClassifiedSeparately() {
        val diagnosis = diagnoseAiSummaryResponse(
            AiSummaryResponse(
                code = 0,
                data = AiSummaryData(
                    code = 1,
                    stid = ""
                )
            )
        )

        assertEquals(AiSummaryFetchStatus.NO_SPEECH, diagnosis.status)
        assertFalse(diagnosis.shouldRetryLater)
    }

    @Test
    fun validSummaryPayloadIsAvailable() {
        val diagnosis = diagnoseAiSummaryResponse(
            AiSummaryResponse(
                code = 0,
                data = AiSummaryData(
                    code = 0,
                    modelResult = AiModelResult(summary = "摘要")
                )
            )
        )

        assertEquals(AiSummaryFetchStatus.AVAILABLE, diagnosis.status)
    }

    @Test
    fun emptySummaryPayloadFallsBackToNoSummary() {
        val diagnosis = diagnoseAiSummaryResponse(
            AiSummaryResponse(
                code = 0,
                data = AiSummaryData(
                    code = 0,
                    modelResult = AiModelResult(
                        summary = "",
                        outline = emptyList()
                    )
                )
            )
        )

        assertEquals(AiSummaryFetchStatus.NO_SUMMARY, diagnosis.status)
    }

    @Test
    fun http412IsMarkedRetryable() {
        val response = Response.error<Any>(
            412,
            "Precondition Failed".toResponseBody("text/plain".toMediaType())
        )

        val diagnosis = diagnoseAiSummaryFailure(HttpException(response))

        assertEquals(AiSummaryFetchStatus.RETRYABLE_FAILURE, diagnosis.status)
        assertTrue(diagnosis.shouldRetryRequest)
    }

    @Test
    fun temporaryNetworkFailureIsMarkedRetryable() {
        val diagnosis = diagnoseAiSummaryFailure(IOException("connection reset"))

        assertEquals(AiSummaryFetchStatus.RETRYABLE_FAILURE, diagnosis.status)
        assertTrue(diagnosis.shouldRetryRequest)
    }

    @Test
    fun serverFailureIsMarkedRetryable() {
        val response = Response.error<Any>(
            503,
            "Service Unavailable".toResponseBody("text/plain".toMediaType())
        )

        val diagnosis = diagnoseAiSummaryFailure(HttpException(response))

        assertEquals(AiSummaryFetchStatus.RETRYABLE_FAILURE, diagnosis.status)
        assertTrue(diagnosis.shouldRetryRequest)
    }
}
