package com.android.purebilibili.feature.video.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PbpProgressPolicyTest {

    @Test
    fun parsePbpProgressData_readsStepAndDefaultEventsFromApiPayload() {
        val data = parsePbpProgressData(
            """
            {
              "step_sec": 3,
              "events": {
                "default": [0, 8853, 8011, 0]
              }
            }
            """.trimIndent()
        )

        assertEquals(3, data.stepSeconds)
        assertEquals(listOf(0f, 8853f, 8011f, 0f), data.values)
    }

    @Test
    fun normalizePbpProgressValues_keepsEmptyAndScalesPeakToOne() {
        assertTrue(normalizePbpProgressValues(emptyList()).isEmpty())
        assertEquals(
            listOf(0f, 0.5f, 1f),
            normalizePbpProgressValues(listOf(0f, 10f, 20f))
        )
    }

    @Test
    fun buildPbpRidgeSamples_mapsNormalizedEventsAcrossDuration() {
        val samples = buildPbpRidgeSamples(
            data = PbpProgressData(
                stepSeconds = 3,
                values = listOf(0f, 5f, 10f)
            ),
            durationMs = 9_000L
        )

        assertEquals(3, samples.size)
        assertEquals(0f, samples[0].fraction)
        assertEquals(0f, samples[0].intensity)
        assertEquals(1f / 3f, samples[1].fraction)
        assertEquals(0.5f, samples[1].intensity)
        assertEquals(2f / 3f, samples[2].fraction)
        assertEquals(1f, samples[2].intensity)
    }

    @Test
    fun buildPbpRidgeSamples_mapsDensityBucketsFromNormalizedIntensity() {
        val samples = buildPbpRidgeSamples(
            data = PbpProgressData(
                stepSeconds = 1,
                values = listOf(0f, 2.9f, 3f, 6.9f, 7f, 10f)
            ),
            durationMs = 10_000L
        )

        assertEquals(
            listOf(
                PbpRidgeDensity.QUIET,
                PbpRidgeDensity.QUIET,
                PbpRidgeDensity.NORMAL,
                PbpRidgeDensity.NORMAL,
                PbpRidgeDensity.HOT,
                PbpRidgeDensity.HOT
            ),
            samples.map { it.density }
        )
    }

    @Test
    fun buildPbpRidgeSamples_ignoresInvalidDataAndClampsFractions() {
        assertTrue(
            buildPbpRidgeSamples(
                data = PbpProgressData(stepSeconds = 0, values = listOf(1f)),
                durationMs = 10_000L
            ).isEmpty()
        )

        val samples = buildPbpRidgeSamples(
            data = PbpProgressData(stepSeconds = 10, values = listOf(1f, 1f, 1f)),
            durationMs = 5_000L
        )

        assertEquals(listOf(0f, 1f, 1f), samples.map { it.fraction })
    }
}
