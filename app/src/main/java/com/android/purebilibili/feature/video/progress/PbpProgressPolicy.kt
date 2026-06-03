package com.android.purebilibili.feature.video.progress

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class PbpProgressData(
    val stepSeconds: Int,
    val values: List<Float>
)

enum class PbpRidgeDensity {
    QUIET,
    NORMAL,
    HOT
}

data class PbpRidgeSample(
    val fraction: Float,
    val intensity: Float,
    val density: PbpRidgeDensity
)

private val PBP_JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun parsePbpProgressData(rawJson: String): PbpProgressData {
    if (rawJson.isBlank()) return PbpProgressData(stepSeconds = 0, values = emptyList())
    return runCatching {
        val root = PBP_JSON.parseToJsonElement(rawJson).jsonObject
        val stepSeconds = root["step_sec"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0
        val values = root["events"]
            ?.jsonObject
            ?.get("default")
            ?.asJsonArrayOrNull()
            .orEmpty()
            .mapNotNull { value -> value.jsonPrimitive.floatOrNull }
        PbpProgressData(
            stepSeconds = stepSeconds,
            values = values
        )
    }.getOrElse {
        PbpProgressData(stepSeconds = 0, values = emptyList())
    }
}

fun normalizePbpProgressValues(values: List<Float>): List<Float> {
    if (values.isEmpty()) return emptyList()
    val peak = values.maxOrNull()?.takeIf { it > 0f } ?: return values.map { 0f }
    return values.map { value -> (value / peak).coerceIn(0f, 1f) }
}

fun resolvePbpRidgeDensity(normalizedIntensity: Float): PbpRidgeDensity {
    val value = normalizedIntensity.coerceIn(0f, 1f)
    return when {
        value < 0.30f -> PbpRidgeDensity.QUIET
        value < 0.70f -> PbpRidgeDensity.NORMAL
        else -> PbpRidgeDensity.HOT
    }
}

fun buildPbpRidgeSamples(
    data: PbpProgressData,
    durationMs: Long
): List<PbpRidgeSample> {
    if (data.stepSeconds <= 0 || durationMs <= 0L || data.values.isEmpty()) {
        return emptyList()
    }
    val normalizedValues = normalizePbpProgressValues(data.values)
    val stepMs = data.stepSeconds * 1000L
    return normalizedValues.mapIndexed { index, intensity ->
        val sampleTimeMs = index * stepMs
        PbpRidgeSample(
            fraction = (sampleTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
            intensity = intensity.coerceIn(0f, 1f),
            density = resolvePbpRidgeDensity(intensity)
        )
    }
}

private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> {
    return this ?: emptyList()
}

private fun kotlinx.serialization.json.JsonElement.asJsonArrayOrNull(): JsonArray? {
    return this as? JsonArray
}
