package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import top.yukonga.miuix.kmp.anim.folmeSpring

enum class VerticalContentRevealMode {
    DefaultExpand,
    FloatUp
}

data class VerticalContentRevealMotionSpec(
    val mode: VerticalContentRevealMode,
    val delayMillis: Int,
    val durationMillis: Int,
    val slideOffsetDp: Float,
    val initialScale: Float
)

fun resolveCommentVerticalContentRevealMotionSpec(): VerticalContentRevealMotionSpec {
    return VerticalContentRevealMotionSpec(
        mode = VerticalContentRevealMode.DefaultExpand,
        delayMillis = 0,
        durationMillis = 0,
        slideOffsetDp = 0f,
        initialScale = 1f
    )
}

fun resolveDetailVerticalContentRevealMotionSpec(
    delayMillis: Int,
    durationMillis: Int,
    slideOffsetDp: Float,
    initialScale: Float
): VerticalContentRevealMotionSpec {
    return VerticalContentRevealMotionSpec(
        mode = VerticalContentRevealMode.FloatUp,
        delayMillis = delayMillis,
        durationMillis = durationMillis,
        slideOffsetDp = slideOffsetDp,
        initialScale = initialScale
    )
}

fun verticalContentRevealEnterTransition(
    spec: VerticalContentRevealMotionSpec
): EnterTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> expandVertically() + fadeIn()
        VerticalContentRevealMode.FloatUp -> fadeIn()
    }
}

fun verticalContentRevealExitTransition(
    spec: VerticalContentRevealMotionSpec
): ExitTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> shrinkVertically() + fadeOut()
        VerticalContentRevealMode.FloatUp -> fadeOut()
    }
}

/** MIUIX Folme 弹性规格（AI 总结卡片展开同款，damping=1 的顺滑收敛）。 */
fun <T> folmeExpandSpringSpec(): SpringSpec<T> = folmeSpring(damping = 1f, response = 0.35f)

fun <T> folmeExpandFadeSpec(): SpringSpec<T> = folmeSpring(damping = 1f, response = 0.25f)

/** 展开进入：MIUIX 风格走 Folme 弹性，其余走默认规格。 */
fun folmeExpandEnterTransition(useMiuixSpring: Boolean): EnterTransition {
    return if (useMiuixSpring) {
        expandVertically(
            animationSpec = folmeExpandSpringSpec(),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = folmeExpandFadeSpec())
    } else {
        fadeIn() + expandVertically()
    }
}

/** 收起退出：与展开进入对称。 */
fun folmeExpandExitTransition(useMiuixSpring: Boolean): ExitTransition {
    return if (useMiuixSpring) {
        shrinkVertically(
            animationSpec = folmeExpandSpringSpec(),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = folmeExpandFadeSpec())
    } else {
        fadeOut() + shrinkVertically()
    }
}
