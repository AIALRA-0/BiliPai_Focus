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

    @Test
    fun buildDanmakuDensityValues_bucketsPositionsByAdaptiveStep() {
        // 600s 视频 -> step = max(2, min(10, 600/240=2)) = 2s，桶数 = 300
        val values = buildDanmakuDensityValues(
            positionsMs = listOf(0L, 1_999L, 2_000L, 5_000L, 599_000L),
            durationSeconds = 600L
        )
        assertEquals(300, values.size)
        assertEquals(2f, values[0])
        assertEquals(1f, values[1])
        assertEquals(1f, values[2])
        assertEquals(0f, values[3])
        assertEquals(1f, values.last())

        // 超出时长的位置被丢弃
        val clamped = buildDanmakuDensityValues(
            positionsMs = listOf(0L, 700_000L, -1L),
            durationSeconds = 600L
        )
        assertEquals(300, clamped.size)
        assertEquals(1f, clamped[0])
        assertTrue(clamped.drop(1).all { it == 0f })
    }

    @Test
    fun buildDanmakuDensityValues_stepClampsToTenSecondsForLongVideos() {
        val values = buildDanmakuDensityValues(
            positionsMs = listOf(0L, 60_000L),
            durationSeconds = 10_800L // 3h -> step = 10s
        )
        assertEquals(1080, values.size)
        assertEquals(2f, values[6]) // 60s / 10s = bucket 6
    }

    @Test
    fun buildDanmakuDensityValues_rejectsInvalidInput() {
        assertTrue(buildDanmakuDensityValues(emptyList(), durationSeconds = 0L).isEmpty())
        assertTrue(buildDanmakuDensityValues(emptyList(), durationSeconds = 100L, stepSeconds = 0).isEmpty())
    }
}
