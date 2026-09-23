package com.android.purebilibili.core.util

import android.util.Rational

/**
 * Android Picture-in-Picture (PiP) allows aspect ratios between 1:2.39 (inclusive) and 2.39:1 (inclusive).
 * https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setAspectRatio(android.util.Rational)
 */
internal const val PIP_MAX_ASPECT_RATIO = 2.39f
internal const val PIP_MIN_ASPECT_RATIO = 1f / 2.39f

/**
 * Pure Kotlin representation of an aspect ratio fraction, testable without Android framework stubs.
 */
internal data class PipRational(val numerator: Int, val denominator: Int) {
    val ratio: Float get() = if (denominator == 0) 0f else numerator.toFloat() / denominator.toFloat()
    fun toAndroidRational(): Rational = Rational(numerator, denominator)
}

/**
 * Computes greatest common divisor using Euclidean algorithm to simplify Rational fractions.
 */
internal tailrec fun greatestCommonDivisor(a: Int, b: Int): Int {
    val absA = kotlin.math.abs(a)
    val absB = kotlin.math.abs(b)
    return if (absB == 0) absA.coerceAtLeast(1) else greatestCommonDivisor(absB, absA % absB)
}

/**
 * Resolves a safe aspect ratio for PictureInPictureParams.
 * Guarantees that the resulting aspect ratio is strictly within Android's required [1/2.39, 2.39] bounds.
 */
internal fun resolveSafePipRational(videoWidth: Int, videoHeight: Int): PipRational {
    if (videoWidth <= 0 || videoHeight <= 0) {
        return PipRational(16, 9)
    }

    val ratio = videoWidth.toFloat() / videoHeight.toFloat()
    return when {
        ratio > PIP_MAX_ASPECT_RATIO -> PipRational(239, 100)
        ratio < PIP_MIN_ASPECT_RATIO -> PipRational(100, 239)
        else -> {
            val gcd = greatestCommonDivisor(videoWidth, videoHeight)
            val num = (videoWidth / gcd).coerceIn(1, 10000)
            val den = (videoHeight / gcd).coerceIn(1, 10000)
            PipRational(num, den)
        }
    }
}

/**
 * Convenience helper returning Android framework [Rational].
 */
internal fun resolveSafeAndroidPipRational(videoWidth: Int, videoHeight: Int): Rational {
    return resolveSafePipRational(videoWidth, videoHeight).toAndroidRational()
}
