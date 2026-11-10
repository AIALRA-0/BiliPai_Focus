package com.android.purebilibili.feature.video.ui.overlay

import java.net.URI
import kotlin.math.log10
import kotlin.math.roundToInt
import com.android.purebilibili.feature.video.viewmodel.normalizeCodecFamilyKey

/** Stream codec strings may contain several profiles of the same persisted codec preference. */
internal fun resolvePiliCodecPreferenceOptions(streamCodecIds: List<String>): List<String> = buildList {
    add("avc1")
    add("hev1")
    add("av01")
    for (codec in streamCodecIds) {
        val family = normalizeCodecFamilyKey(codec) ?: continue
        if (family !in this) add(family)
    }
}

internal fun normalizePlayerAmplification(value: Float): Float =
    if (value.isFinite()) (value.coerceIn(1f, 3f) * 20f).roundToInt() / 20f else 1f

/** LoudnessEnhancer accepts millibels, not a linear amplitude percentage. */
internal fun playerAmplificationMillibels(value: Float): Int =
    (2000.0 * log10(normalizePlayerAmplification(value).toDouble())).roundToInt()

internal fun isEditablePlaybackUrl(value: String, allowEmpty: Boolean = false): Boolean {
    if (value.isBlank()) return allowEmpty
    val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
    return (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank()
}

internal fun matchesPiliPlaybackIdentity(
    expectedBvid: String, expectedCid: Long, currentBvid: String, currentCid: Long
): Boolean = expectedBvid == currentBvid && expectedCid == currentCid
