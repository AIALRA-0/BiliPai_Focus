package com.android.purebilibili.core.ui.adaptive

const val RUNTIME_VISUAL_GUARD_HIGH_JANK_THRESHOLD_PERCENT = 7.5f
const val RUNTIME_VISUAL_GUARD_RECOVER_JANK_THRESHOLD_PERCENT = 4.0f
const val RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS = 60_000L
private const val REQUIRED_HIGH_JANK_WINDOWS = 2

fun isRuntimeVisualGuardHighJankWindow(jankPercent: Float): Boolean =
    jankPercent >= RUNTIME_VISUAL_GUARD_HIGH_JANK_THRESHOLD_PERCENT

data class RuntimeVisualGuardDecision(
    val effectiveMotionTier: MotionTier,
    val forceLowBlurBudget: Boolean,
    val downgraded: Boolean,
    val nextLastDowngradeAtMs: Long?
)

fun resolveRuntimeVisualGuardDecision(
    enabled: Boolean,
    baseTier: MotionTier,
    rollingJankPercent: Float,
    consecutiveHighJankWindows: Int,
    lastDowngradeAtMs: Long?,
    nowMs: Long
): RuntimeVisualGuardDecision {
    if (!enabled) {
        return RuntimeVisualGuardDecision(
            effectiveMotionTier = baseTier,
            forceLowBlurBudget = false,
            downgraded = false,
            nextLastDowngradeAtMs = lastDowngradeAtMs
        )
    }

    val shouldTriggerDowngrade =
        isRuntimeVisualGuardHighJankWindow(rollingJankPercent) &&
            consecutiveHighJankWindows >= REQUIRED_HIGH_JANK_WINDOWS
    if (shouldTriggerDowngrade) {
        return RuntimeVisualGuardDecision(
            effectiveMotionTier = MotionTier.Reduced,
            forceLowBlurBudget = true,
            downgraded = true,
            nextLastDowngradeAtMs = nowMs
        )
    }

    val inCooldown = lastDowngradeAtMs != null &&
        (nowMs - lastDowngradeAtMs) < RUNTIME_VISUAL_GUARD_DOWNGRADE_COOLDOWN_MS
    if (inCooldown) {
        return RuntimeVisualGuardDecision(
            effectiveMotionTier = MotionTier.Reduced,
            forceLowBlurBudget = true,
            downgraded = true,
            nextLastDowngradeAtMs = lastDowngradeAtMs
        )
    }

    val shouldStayDowngraded =
        lastDowngradeAtMs != null &&
            rollingJankPercent > RUNTIME_VISUAL_GUARD_RECOVER_JANK_THRESHOLD_PERCENT
    if (shouldStayDowngraded) {
        return RuntimeVisualGuardDecision(
            effectiveMotionTier = MotionTier.Reduced,
            forceLowBlurBudget = true,
            downgraded = true,
            nextLastDowngradeAtMs = lastDowngradeAtMs
        )
    }

    return RuntimeVisualGuardDecision(
        effectiveMotionTier = baseTier,
        forceLowBlurBudget = false,
        downgraded = false,
        nextLastDowngradeAtMs = lastDowngradeAtMs
    )
}

