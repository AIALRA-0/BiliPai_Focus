/*
 * Copyright 2023 Google LLC
 * Licensed under the Apache License, Version 2.0.
 * See assets/licenses/material_color_utilities.LICENSE.
 *
 * Native port of flex_seed_scheme 5.0.1's MCU DynamicColor.getTone.
 */
@file:Suppress("DEPRECATION")

package com.android.purebilibili.core.theme

import com.materialkolor.contrast.Contrast
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.ToneDeltaPair.TonePolarity
import com.materialkolor.dynamiccolor.ToneDeltaPair.DeltaConstraint
import com.materialkolor.dynamiccolor.DynamicScheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

/** Keep the reference constraint solver independent of later MCU spec changes. */
internal class SeedToneResolver(private val scheme: DynamicScheme) {
    private val cache = mutableMapOf<String, Double>()

    fun tone(role: DynamicColor): Double = cache.getOrPut(role.name) { resolve(role) }

    private fun resolve(role: DynamicColor): Double {
        val decreasing = scheme.contrastLevel < 0.0
        val pair = role.toneDeltaPair?.invoke(scheme)
        if (pair != null) {
            val background = requireNotNull(role.background?.invoke(scheme))
            val bgTone = tone(background)
            val aIsNearer = pair.constraint == DeltaConstraint.NEARER ||
                (pair.polarity == TonePolarity.LIGHTER && !scheme.isDark) ||
                (pair.polarity == TonePolarity.DARKER && scheme.isDark)
            val nearer = if (aIsNearer) pair.roleA else pair.roleB
            val farther = if (aIsNearer) pair.roleB else pair.roleA
            val direction = if (scheme.isDark) 1.0 else -1.0
            val nContrast = requireNotNull(nearer.contrastCurve?.invoke(scheme)).get(scheme.contrastLevel)
            val fContrast = requireNotNull(farther.contrastCurve?.invoke(scheme)).get(scheme.contrastLevel)
            val nInitial = nearer.tone(scheme)
            val fInitial = farther.tone(scheme)
            var nTone = if (!decreasing && Contrast.ratioOfTones(bgTone, nInitial) >= nContrast) nInitial
                else foregroundTone(bgTone, nContrast)
            var fTone = if (!decreasing && Contrast.ratioOfTones(bgTone, fInitial) >= fContrast) fInitial
                else foregroundTone(bgTone, fContrast)
            if ((fTone - nTone) * direction < pair.delta) {
                fTone = (nTone + pair.delta * direction).coerceIn(0.0, 100.0)
                if ((fTone - nTone) * direction < pair.delta) {
                    nTone = (fTone - pair.delta * direction).coerceIn(0.0, 100.0)
                }
            }
            if (nTone >= 50.0 && nTone < 60.0) {
                if (direction > 0.0) {
                    nTone = 60.0
                    fTone = max(fTone, nTone + pair.delta * direction)
                } else {
                    nTone = 49.0
                    fTone = min(fTone, nTone + pair.delta * direction)
                }
            } else if (fTone >= 50.0 && fTone < 60.0) {
                if (pair.stayTogether) {
                    if (direction > 0.0) {
                        nTone = 60.0
                        fTone = max(fTone, nTone + pair.delta * direction)
                    } else {
                        nTone = 49.0
                        fTone = min(fTone, nTone + pair.delta * direction)
                    }
                } else {
                    fTone = if (direction > 0.0) 60.0 else 49.0
                }
            }
            return if (role.name == nearer.name) nTone else fTone
        }
        var answer = role.tone(scheme)
        val background = role.background?.invoke(scheme) ?: return answer
        val bgTone = tone(background)
        val ratio = requireNotNull(role.contrastCurve?.invoke(scheme)).get(scheme.contrastLevel)
        if (decreasing || Contrast.ratioOfTones(bgTone, answer) < ratio) {
            answer = foregroundTone(bgTone, ratio)
        }
        if (role.isBackground && answer >= 50.0 && answer < 60.0) {
            answer = if (Contrast.ratioOfTones(49.0, bgTone) >= ratio) 49.0 else 60.0
        }
        val second = role.secondBackground?.invoke(scheme) ?: return answer
        val secondTone = tone(second)
        val upper = max(bgTone, secondTone)
        val lower = min(bgTone, secondTone)
        if (Contrast.ratioOfTones(upper, answer) >= ratio && Contrast.ratioOfTones(lower, answer) >= ratio) {
            return answer
        }
        val light = Contrast.lighter(upper, ratio) ?: -1.0
        val dark = Contrast.darker(lower, ratio) ?: -1.0
        if (tonePrefersLightForeground(bgTone) || tonePrefersLightForeground(secondTone)) {
            return if (light < 0.0) 100.0 else light
        }
        if (light >= 0.0 && dark < 0.0) return light
        if (dark >= 0.0 && light < 0.0) return dark
        return if (dark < 0.0) 0.0 else dark
    }

    // MCU 6 makes these helpers internal. Keep the legacy preset solver's exact
    // foreground choice here instead of depending on an unstable internal API.
    private fun tonePrefersLightForeground(tone: Double): Boolean = tone.roundToInt() < 60

    private fun foregroundTone(backgroundTone: Double, ratio: Double): Double {
        val lighter = Contrast.lighterUnsafe(backgroundTone, ratio)
        val darker = Contrast.darkerUnsafe(backgroundTone, ratio)
        val lighterRatio = Contrast.ratioOfTones(lighter, backgroundTone)
        val darkerRatio = Contrast.ratioOfTones(darker, backgroundTone)
        return if (tonePrefersLightForeground(backgroundTone)) {
            val negligibleDifference = abs(lighterRatio - darkerRatio) < 0.1 &&
                lighterRatio < ratio && darkerRatio < ratio
            if (lighterRatio >= ratio || lighterRatio >= darkerRatio || negligibleDifference) lighter else darker
        } else {
            if (darkerRatio >= ratio || darkerRatio >= lighterRatio) darker else lighter
        }
    }

}
