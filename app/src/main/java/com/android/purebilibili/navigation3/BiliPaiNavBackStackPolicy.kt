package com.android.purebilibili.navigation3

internal fun resolveInitialBiliPaiBackStack(
    firstRoute: String?,
    onboardingRequired: Boolean
): List<BiliPaiNavKey> {
    if (onboardingRequired) {
        return listOf(BiliPaiNavKey.Onboarding)
    }
    return listOf(BiliPaiNavKey.MainHost)
}

internal fun pushBiliPaiNavKey(
    currentStack: List<BiliPaiNavKey>,
    key: BiliPaiNavKey
): List<BiliPaiNavKey> {
    val base = currentStack.ifEmpty { listOf(BiliPaiNavKey.MainHost) }
    return if (base.last() == key) base else base + key
}

internal fun popBiliPaiNavKey(
    currentStack: List<BiliPaiNavKey>
): List<BiliPaiNavKey> {
    return if (currentStack.size <= 1) currentStack else currentStack.dropLast(1)
}

/**
 * 弹出栈顶所有非 [BiliPaiNavKey.MainHost] 条目，恢复到根。常用于「返回首页」入口：
 * 把视频详情（以及中间夹杂的搜索、登录等）一次性清理掉，由 popTransitionSpec 一次播放横向过渡。
 *
 * - 栈为空 → 维持空（调用方会兜底为 `[MainHost]`）
 * - 栈底已经是 MainHost 且只剩它 → 维持原样
 * - 栈底不是 MainHost（异常态）→ 维持原样，避免误删
 */
internal fun popBiliPaiNavKeyToRoot(
    currentStack: List<BiliPaiNavKey>
): List<BiliPaiNavKey> {
    if (currentStack.isEmpty()) return currentStack
    val root = currentStack.first()
    if (root != BiliPaiNavKey.MainHost) return currentStack
    return listOf(root)
}
