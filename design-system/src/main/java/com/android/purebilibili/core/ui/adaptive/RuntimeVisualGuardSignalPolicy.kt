package com.android.purebilibili.core.ui.adaptive

/**
 * 纳入运行时视觉守卫的 jank 打点前缀。
 *
 * 只收「会掉帧且用户能直接感知」的连续交互：转场、竖滑、横滑。
 * 纯语义标记（`home:current_category`、`video_player:gesture_mode`）不开窗口——
 * 它们在界面静止时也长期挂着，会把静止帧算进分母，稀释真实掉帧率。
 */
val RUNTIME_VISUAL_GUARD_TRACKED_PREFIXES: List<String> = listOf(
    "VideoCardTransition",
    "home:feed:",
    "home:pager_swipe",
    "home:header_transition",
    "video_detail:tab_swipe",
    "video_detail:intro_scroll",
    "video_detail:comment_scroll",
    "video_detail:player_swipe_collapse",
)

fun isRuntimeVisualGuardTrackedStateKey(key: String): Boolean {
    if (key.isBlank()) return false
    return RUNTIME_VISUAL_GUARD_TRACKED_PREFIXES.any { prefix ->
        key == prefix || key.startsWith(prefix)
    }
}

/**
 * 取更保守（更低）的档位。
 *
 * 依赖 [MotionTier] 的声明顺序 `Reduced < Normal < Enhanced`；
 * [motionTierOrderIsConservativeFirst] 会在 enum 被重排时立刻失败。
 */
fun minMotionTier(first: MotionTier, second: MotionTier): MotionTier =
    if (first.ordinal <= second.ordinal) first else second

fun motionTierOrderIsConservativeFirst(): Boolean =
    MotionTier.Reduced.ordinal < MotionTier.Normal.ordinal &&
        MotionTier.Normal.ordinal < MotionTier.Enhanced.ordinal

/**
 * 合并多个信号各自的判定：任一信号降级 → 全局降级。
 *
 * 单个信号的窗口互相独立（竖滑与横滑会同帧共存，混在一个窗口里会互相污染分母），
 * 但对外只暴露一个决策，且取最保守的那个。
 */
fun mergeRuntimeVisualGuardDecisions(
    decisions: Collection<RuntimeVisualGuardDecision>,
    baseTier: MotionTier,
): RuntimeVisualGuardDecision {
    val downgraded = decisions.filter { it.downgraded }
    if (downgraded.isEmpty()) {
        return RuntimeVisualGuardDecision(
            effectiveMotionTier = baseTier,
            forceLowBlurBudget = false,
            downgraded = false,
            nextLastDowngradeAtMs = null,
        )
    }
    return RuntimeVisualGuardDecision(
        effectiveMotionTier = downgraded.fold(baseTier) { tier, decision ->
            minMotionTier(tier, decision.effectiveMotionTier)
        },
        forceLowBlurBudget = downgraded.any { it.forceLowBlurBudget },
        downgraded = true,
        nextLastDowngradeAtMs = downgraded.mapNotNull { it.nextLastDowngradeAtMs }.maxOrNull(),
    )
}
