package com.android.purebilibili.core.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlinx.coroutines.delay

data class EnterMotionPolicy(
    val staggerStepMs: Int,
    val maxStaggerMs: Int,
    val initialScale: Float,
    val translationFactor: Float,
    val dampingRatio: Float,
    val stiffness: Float
)

fun resolveEnterMotionPolicy(motionTier: MotionTier): EnterMotionPolicy {
    return when (motionTier) {
        MotionTier.Reduced -> EnterMotionPolicy(
            staggerStepMs = 10,
            maxStaggerMs = 60,
            initialScale = 0.97f,
            translationFactor = 0.35f,
            dampingRatio = 0.92f,
            stiffness = 720f
        )

        MotionTier.Enhanced -> EnterMotionPolicy(
            staggerStepMs = 24,
            maxStaggerMs = 180,
            initialScale = 0.88f,
            translationFactor = 1.1f,
            dampingRatio = 0.62f,
            stiffness = 320f
        )

        MotionTier.Normal -> EnterMotionPolicy(
            staggerStepMs = 30,
            maxStaggerMs = 200,
            initialScale = 0.9f,
            translationFactor = 1f,
            dampingRatio = 0.7f,
            stiffness = 350f
        )
    }
}

/**
 *  列表项进场动画 (Premium 非线性动画)
 *
 * 特点：
 * - 交错延迟实现波浪效果
 * - 从下方滑入 + 缩放 + 淡入
 * - 非线性缓动曲线 (FastOutSlowIn)
 * - Q弹果冻回弹效果
 *
 * @param index: 列表项的索引，用于计算延迟时间
 * @param key: 用于触发重置动画的键值 (通常传视频ID)
 * @param initialOffsetY: 初始 Y 偏移量
 * @param animationEnabled: 是否启用动画 (设置开关)
 */
fun Modifier.animateEnter(
    index: Int = 0,
    key: Any? = Unit,
    initialOffsetY: Float = 60f,
    animationEnabled: Boolean = true,
    motionTier: MotionTier = MotionTier.Normal
): Modifier = composed {

    // 动画被禁用时直接返回无动画效果
    if (!animationEnabled) {
        return@composed this
    }

    val motionPolicy = remember(motionTier) { resolveEnterMotionPolicy(motionTier) }

    // 使用单一进度值驱动所有动画属性，减少内存分配和协程开销
    var animationStarted by remember(key) { mutableStateOf(false) }

    // 计算交错延迟（按 MotionTier 分档）
    val delayMs = (index * motionPolicy.staggerStepMs).coerceAtMost(motionPolicy.maxStaggerMs)

    LaunchedEffect(key) {
        delay(delayMs.toLong())
        animationStarted = true
    }

    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = motionPolicy.dampingRatio,
            stiffness = motionPolicy.stiffness
        ),
        label = "enterProgress"
    )

    this.graphicsLayer {
        alpha = progress
        translationY = (initialOffsetY * motionPolicy.translationFactor) * (1f - progress)
        scaleX = motionPolicy.initialScale + (1f - motionPolicy.initialScale) * progress
        scaleY = motionPolicy.initialScale + (1f - motionPolicy.initialScale) * progress
    }
}

/**
 * Q弹点击效果 (按压缩放)
 */
fun Modifier.bouncyClickable(
    scaleDown: Float = 0.90f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
