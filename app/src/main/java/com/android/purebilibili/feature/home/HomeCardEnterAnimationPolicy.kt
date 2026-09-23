package com.android.purebilibili.feature.home

/**
 * 首页卡片进场动画在「挂载瞬间」的门控。
 *
 * 流畅度优先：
 * - 开关关闭 → 不播
 * - 从详情返回 / 切分类 → 不播，避免和 shared 过渡叠双重动效
 * - 列表正在滚动时挂载（Lazy 复用进屏）→ 不播，避免快滑时多卡并发 spring
 *
 * 注意：判定只在卡片首次 composition 读取一次（见 call site 的 remember(bvid)），
 * 因此「滚动中进屏」永久跳过该次挂载的进场，滚动停止也不会补播——这是刻意取舍。
 */
internal fun resolveHomeCardEnterAnimationEnabledAtMount(
    baseAnimationEnabled: Boolean,
    isReturningFromDetail: Boolean,
    isSwitchingCategory: Boolean,
    isScrollInProgress: Boolean = false
): Boolean {
    if (!baseAnimationEnabled) return false
    if (isReturningFromDetail) return false
    if (isSwitchingCategory) return false
    if (isScrollInProgress) return false
    return true
}

/**
 * 进场是否应与共享元素过渡「协同」：弱化位移/缩放，只保留轻量淡入。
 * 两开关同时开时避免 graphicsLayer 几何变换污染 sharedBounds 源 bounds。
 */
internal fun shouldCoordinateCardEnterWithSharedTransition(
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean
): Boolean = cardAnimationEnabled && cardTransitionEnabled
