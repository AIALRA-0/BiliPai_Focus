package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier

internal fun resolveAnimationSettingsCardMotionTier(
    baseTier: MotionTier,
    cardAnimationEnabled: Boolean
): MotionTier = resolveEffectiveMotionTier(
    baseTier = baseTier,
    animationEnabled = cardAnimationEnabled
)
