package top.yukonga.miuix.kmp.nav.runtime

import androidx.compose.animation.core.Easing

/**
 * 固定时长的松手交接：端点为 0/1，起始斜率来自释放速度，末端斜率为 0。
 * 使用三次 Hermite 插值；归一化斜率限制在 0..3，保持单调且不越过卸层边界。
 * 朝目标的速度在可表达范围内精确接续；反向及过快速度限制为安全端点。
 */
internal class NavVelocityHandoffEasing(
    distance: Float,
    initialVelocity: Float,
    durationMillis: Int,
) : Easing {
    private val slope = if (distance.isFinite() && distance != 0f &&
        initialVelocity.isFinite() && durationMillis > 0
    ) {
        (initialVelocity * (durationMillis / 1000f) / distance).coerceIn(0f, 3f)
    } else 0f

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        val t = fraction.coerceIn(0f, 1f)
        return (slope * t + (3f - 2f * slope) * t * t +
            (slope - 2f) * t * t * t).coerceIn(0f, 1f)
    }
}
