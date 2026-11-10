package com.android.purebilibili.navigation3

internal data class LiveAvatarContainerContentFrame(
    val sourceScale: Float,
    val sourceAlpha: Float,
) {
    val contentAlpha: Float get() = 1f - sourceAlpha
}

/** 头像与昵称、直播标记一起短程淡出；进场、返回和取消按同一容器比例取值。 */
internal fun resolveLiveAvatarContainerContentFrame(containerScale: Float): LiveAvatarContainerContentFrame {
    if (!containerScale.isFinite() || containerScale <= 0f) {
        return LiveAvatarContainerContentFrame(sourceScale = 1f, sourceAlpha = 0f)
    }
    // 页面容器可以继续展开，但头像和原始字形最多放大 20%。
    val handoff = ((containerScale - 1.08f) / (1.6f - 1.08f)).coerceIn(0f, 1f)
    return LiveAvatarContainerContentFrame(
        sourceScale = containerScale.coerceAtMost(1.2f),
        sourceAlpha = 1f - handoff * handoff * (3f - 2f * handoff),
    )
}
