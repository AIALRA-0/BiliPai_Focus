// 文件路径: core/ui/animation/DampedDragAnimation.kt
package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastCoerceIn
import com.android.purebilibili.core.ui.motion.BottomBarMotionSpec
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

internal fun resolveDampedDragVelocityItemsPerSecond(
    velocityPxPerSecond: Float,
    itemWidthPx: Float
): Float {
    if (itemWidthPx <= 0f) return 0f
    return velocityPxPerSecond / itemWidthPx
}

internal fun resolveDampedDragReleaseTargetIndex(
    currentValue: Float,
    velocityPxPerSecond: Float,
    itemWidthPx: Float,
    itemCount: Int,
    motionSpec: BottomBarMotionSpec
): Int {
    if (itemCount <= 0) return 0
    val velocityItems = resolveDampedDragVelocityItemsPerSecond(
        velocityPxPerSecond = velocityPxPerSecond,
        itemWidthPx = itemWidthPx
    )
    val projectedValue = currentValue + velocityItems * motionSpec.drag.flingProjectionTimeSeconds
    var nextIndex = projectedValue.roundToInt()
    val baseIndex = currentValue.roundToInt()
    val maxReleaseStep = motionSpec.drag.maxReleaseStepCount.coerceAtLeast(1)
    if (abs(nextIndex - baseIndex) > maxReleaseStep) {
        nextIndex = baseIndex + (nextIndex - baseIndex).sign * maxReleaseStep
    }
    return nextIndex.coerceIn(0, itemCount - 1)
}

/**
 * 🌊 阻尼拖拽动画状态
 * 
 * 实现类似 LiquidBottomTabs 的手势跟随效果：
 * - 拖拽时平滑跟随手指
 * - 释放后弹回吸附到最近选项
 * - 支持速度感知的弹性形变
 */
internal class DampedDragAnimationState(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
    private val onIndexChanged: (Int) -> Unit,
    private val motionSpec: BottomBarMotionSpec
) {
    /** 当前动画值（浮点索引，用于平滑过渡） */
    private val animatable = Animatable(initialIndex.toFloat())
    
    /** 按压进度动画 (0f = 释放, 1f = 按下) — 参考 LiquidBottomTabs */
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    
    /** 累计拖拽偏移量 (px) — 用于面板偏移效果 */
    private val offsetAnimation = Animatable(0f)
    
    /** 当前动画位置 */
    val value: Float get() = animatable.value
    
    /** 当前索引速度（items/s，用于 capsule 形变等索引空间动画） */
    val velocity: Float get() = animatable.velocity

    /** 最近一次释放手势的像素速度（px/s，用于折射/透镜强度） */
    var velocityPxPerSecond by mutableFloatStateOf(0f)
        private set
    
    /** 按压进度 (0f..1f) */
    val pressProgress: Float get() = pressProgressAnimation.value
    
    /** 累计拖拽偏移量 (px) */
    val dragOffset: Float get() = offsetAnimation.value
    
    /** 是否正在拖拽 */
    var isDragging by mutableStateOf(false)
        private set
    
    /** 拖拽时的缩放比例 */
    val scale: Float get() = if (isDragging) 1.1f else 1f
    
    /** 目标索引（释放后吸附的目标） */
    var targetIndex = initialIndex
        private set
    
    /** 动画是否正在运行 */
    val isRunning: Boolean get() = animatable.isRunning

    private var desiredValue = initialIndex.toFloat()
    private var desiredDragOffsetPx = 0f
    private var motionGeneration = 0
    private var positionJob: Job? = null
    private var pressJob: Job? = null
    private var selectionJob: Job? = null
    private var offsetJob: Job? = null

    private fun startNewMotion(): Int {
        motionGeneration += 1
        return motionGeneration
    }

    /**
     * 处理拖拽事件
     * @param dragAmountPx 拖拽像素距离
     * @param itemWidthPx 单个项目宽度（像素）
     */
    fun onDrag(dragAmountPx: Float, itemWidthPx: Float) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        if (!isDragging) {
            isDragging = true
            startNewMotion()
            selectionJob?.cancel()
            positionJob?.cancel()
            offsetJob?.cancel()
            desiredValue = animatable.value
            desiredDragOffsetPx = offsetAnimation.value
            velocityPxPerSecond = 0f
            // 按压缩放 — 参考 LiquidBottomTabs press()
            pressJob?.cancel()
            pressJob = scope.launch {
                pressProgressAnimation.animateTo(1f, motionSpec.drag.pressSpring.toSpringSpec())
            }
        }
        
        // [优化] 橡皮筋阻尼物理：
        val currentValue = desiredValue
        val isOverscrolling = currentValue < 0f || currentValue > (itemCount - 1).toFloat()
        
        // [调整] 提升灵敏度系数 (0.6 -> 1.0) 确保完全跟手
        val baseResistance = motionSpec.drag.baseResistance
        val overscrollResistance = motionSpec.drag.overscrollResistance

        // 允许边缘回弹：放宽限制范围
        val newValue = (
            desiredValue +
                (dragAmountPx / itemWidthPx) *
                if (isOverscrolling) overscrollResistance else baseResistance
            )
            .fastCoerceIn(
                -motionSpec.drag.overscrollLimitItems,
                (itemCount - 1).toFloat() + motionSpec.drag.overscrollLimitItems
            )
        desiredValue = newValue
        
        positionJob?.cancel()
        positionJob = scope.launch {
            animatable.stop()
            animatable.snapTo(newValue)
        }
        // 累计偏移量 — 用于面板偏移
        desiredDragOffsetPx += dragAmountPx
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.stop()
            offsetAnimation.snapTo(desiredDragOffsetPx)
        }
    }
    
    /**
     * 立即跳转到指定位置（无动画）
     */
    fun snapTo(targetValue: Float) {
        // 更新目标索引以防止 offset 累积误差
        val generation = startNewMotion()
        selectionJob?.cancel()
        positionJob?.cancel()
        desiredValue = targetValue
        targetIndex = targetValue.roundToInt().coerceIn(0, itemCount - 1)
        positionJob = scope.launch {
            if (generation != motionGeneration) return@launch
            animatable.stop()
            animatable.snapTo(targetValue)
        }
    }

    /**
     * 处理拖拽结束 (带速度感知)
     * @param velocityX 水平速度 (px/s)
     * @param itemWidthPx 项目宽度 (px)
     */
    fun onDragEnd(velocityX: Float, itemWidthPx: Float) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        isDragging = false
        val generation = motionGeneration
        velocityPxPerSecond = velocityX
        
        val currentValue = desiredValue
        
        // [核心优化] 基于速度的意图判断 (Fling Logic)
        // 1. 计算这一帧的归一化速度 (items/sec)
        val velocityItems = resolveDampedDragVelocityItemsPerSecond(
            velocityPxPerSecond = velocityX,
            itemWidthPx = itemWidthPx
        )
        val releaseTargetIndex = resolveDampedDragReleaseTargetIndex(
            currentValue = currentValue,
            velocityPxPerSecond = velocityX,
            itemWidthPx = itemWidthPx,
            itemCount = itemCount,
            motionSpec = motionSpec
        )
        targetIndex = releaseTargetIndex
        desiredValue = releaseTargetIndex.toFloat()
        
        selectionJob?.cancel()
        selectionJob = scope.launch {
            positionJob?.cancel()
            animatable.animateTo(
                targetValue = releaseTargetIndex.toFloat(),
                animationSpec = motionSpec.drag.selectionSpring.toSpringSpec(),
                initialVelocity = velocityItems
            )
            if (generation == motionGeneration) {
                velocityPxPerSecond = 0f
                onIndexChanged(releaseTargetIndex)
            }
        }
        // 释放按压缩放 — 参考 LiquidBottomTabs release()
        pressJob?.cancel()
        pressJob = scope.launch {
            pressProgressAnimation.animateTo(0f, motionSpec.drag.pressSpring.toSpringSpec())
        }
        // 偏移量归零 — 弹性回弹
        desiredDragOffsetPx = 0f
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.animateTo(0f, motionSpec.drag.offsetSnapSpring.toSpringSpec())
        }
    }
    
    /**
     * 外部更新选中索引（点击选择时）
     */
    fun updateIndex(index: Int) {
        // [修复] 拖拽过程中忽略外部更新，防止动画中断
        if (isDragging) return
        
        // [Fix] Check actual value distance. 
        // If targetIndex matches but we are stuck at an offset (e.g. 2.8 vs 3.0 via snapTo), 
        // we MUST force restart the animation.
        if (index == targetIndex && (isRunning || abs(value - index.toFloat()) < 0.005f)) return
        val generation = startNewMotion()
        selectionJob?.cancel()
        positionJob?.cancel()
        velocityPxPerSecond = 0f
        targetIndex = index
        desiredValue = index.toFloat()
        selectionJob = scope.launch {
            if (generation != motionGeneration) return@launch
            animatable.animateTo(
                targetValue = index.toFloat(),
                animationSpec = motionSpec.drag.selectionSpring.toSpringSpec()
            )
        }
    }
}

/**
 * 创建并记住阻尼拖拽动画状态
 */
@Composable
internal fun rememberDampedDragAnimationState(
    initialIndex: Int,
    itemCount: Int,
    onIndexChanged: (Int) -> Unit,
    motionSpec: BottomBarMotionSpec = resolveBottomBarMotionSpec()
): DampedDragAnimationState {
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)
    
    return remember(itemCount, motionSpec) {
        DampedDragAnimationState(
            initialIndex = initialIndex,
            itemCount = itemCount,
            scope = scope,
            onIndexChanged = { currentOnIndexChanged(it) },
            motionSpec = motionSpec
        )
    }
}

/**
 * 水平拖拽手势 Modifier (带速度追踪)
 */
internal fun Modifier.horizontalDragGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float
): Modifier = this.pointerInput(dragState, itemWidthPx) {
    val velocityTracker = VelocityTracker()
    
    awaitPointerEventScope {
        while (true) {
            // [Fix] Allow gesture to start even if child (clickable) consumed the DOWN event
            val down = awaitFirstDown(requireUnconsumed = false)
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            
            // [Fix] Wait for touch slop before claiming the gesture (to distinguish from tap)
            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                dragState.onDrag(over, itemWidthPx)
            }

            if (dragStart != null) {
                // Drag confirmed
                velocityTracker.addPosition(dragStart.uptimeMillis, dragStart.position)
                
                var isCancelled = false
                
                // Continue handling drag events
                try {
                     horizontalDrag(dragStart.id) { change ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        
                        val dragAmount = change.position.x - change.previousPosition.x
                        dragState.onDrag(dragAmount, itemWidthPx)
                    }
                } catch (e: Exception) {
                    isCancelled = true
                }
                
                // Drag ended
                if (!isCancelled) {
                    val velocity = velocityTracker.calculateVelocity()
                    dragState.onDragEnd(velocity.x, itemWidthPx)
                } else {
                    // Cancelled
                    dragState.onDragEnd(0f, itemWidthPx)
                }
            }
        }
    }
}
