// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.purebilibili.feature.home.components.liquid

import com.android.purebilibili.core.ui.effect.lens as sharedLens
import top.yukonga.miuix.kmp.blur.BackdropEffectScope

/** Keep app callers on the same shared Kyant lens as design-system value controls. */
fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
    chromaticEdgeBand: Float? = null,
    chromaticBandHorizontal: Boolean = false,
) = sharedLens(
    refractionHeight, refractionAmount, depthEffect, chromaticAberration, chromaticEdgeBand,
    chromaticBandHorizontal = chromaticBandHorizontal,
)
