// 文件路径: core/ui/animation/DampedDragAnimation.kt
package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastCoerceIn
import com.android.purebilibili.core.ui.motion.BottomBarMotionSpec
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

/**
 * 根据拖拽速度和位置计算释放后吸附的目标索引（9.0.0 飞掷投影）。
 */
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
 * 共享的 KernelSU 指示器拖拽动画状态。
 *
 * 复刻来源：
 * KernelSU manager FloatingBottomBar / DampedDragAnimation / DragGestureInspector，
 * commit 778fb38bbf0c43f168b8bbd7d9e369d6fb46754b。
 * 底栏、顶部标签、分段控件、分区侧栏共用此内核，避免各自维护一套速度形变。
 *
 * 交互逻辑以 9.0.0 发行版为准（跟手 snapTo + 速度飞掷投影），
 * 拖动手感保持 KSU 风格（KERNEL_SU_PRESSED_SCALE + 速度形变）。
 */
internal class DampedDragAnimationState(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
    private val onIndexChanged: (Int) -> Unit,
    private val motionSpec: BottomBarMotionSpec,
    private val notifyIndexChangedOnReleaseStart: Boolean = false,
    @Suppress("UNUSED_PARAMETER") private val holdPressUntilReleaseTargetSettles: Boolean = false
) {
    private val valueAnimationSpec = spring(1f, 1000f, 0.001f)
    private val velocityAnimationSpec = spring(0.5f, 300f, 0.01f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)
    private val offsetSnapAnimationSpec = spring(1f, 300f, 0.5f)

    private val valueAnimation = Animatable(initialIndex.toFloat(), 0.001f)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(1f, 0.001f)
    private val scaleYAnimation = Animatable(1f, 0.001f)
    private val offsetAnimation = Animatable(0f)
    private val mutatorMutex = MutatorMutex()
    private val deformationVelocityTracker = VelocityTracker()

    private var motionGeneration = 0
    private var valueJob: Job? = null
    private var velocityJob: Job? = null
    private var releaseJob: Job? = null
    private var offsetJob: Job? = null

    /** 9.0.0 风格的拖拽期望位置（允许超滚，不受边界限制） */
    private var desiredValue = initialIndex.toFloat()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val velocity: Float get() = velocityAnimation.value
    val deformationVelocityItemsPerSecond: Float get() = velocityAnimation.value
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val scale: Float get() = maxOf(scaleX, scaleY)
    val dragOffset: Float get() = offsetAnimation.value
    val isRunning: Boolean get() = valueAnimation.isRunning

    var velocityPxPerSecond by mutableFloatStateOf(0f)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var targetIndex = initialIndex
        private set

    var settledReleaseCount by mutableIntStateOf(0)
        private set

    var settledSelectionCount by mutableIntStateOf(0)
        private set

    private fun startNewMotion(): Int {
        motionGeneration += 1
        return motionGeneration
    }

    fun press() {
        deformationVelocityTracker.resetTracking()
        releaseJob?.cancel()
        releaseJob = scope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleYAnimationSpec) }
        }
    }

    fun release(onSettled: (() -> Unit)? = null) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = ((itemCount - 1).toFloat() * 0.025f).coerceAtLeast(0.001f)
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            onSettled?.invoke()
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(1f, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(1f, scaleYAnimationSpec) }
        }
    }

    private fun updateDeformationVelocity(value: Float) {
        val valueRange = (itemCount - 1).toFloat().coerceAtLeast(1f)
        deformationVelocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = deformationVelocityTracker.calculateVelocity().x / valueRange
        velocityJob = scope.launch {
            velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
        }
    }

    fun snapTo(targetValue: Float) {
        val generation = startNewMotion()
        valueJob?.cancel()
        desiredValue = targetValue
        targetIndex = targetValue.roundToInt().coerceIn(0, itemCount - 1)
        scope.launch {
            if (generation != motionGeneration) return@launch
            valueAnimation.stop()
            valueAnimation.snapTo(targetValue)
            velocityAnimation.snapTo(0f)
        }
    }

    fun animateToValue(value: Float, onSettled: (() -> Unit)? = null) {
        scope.launch {
            mutatorMutex.mutate {
                press()
                val nextTarget = value.fastCoerceIn(0f, (itemCount - 1).toFloat())
                targetIndex = nextTarget.roundToInt().coerceIn(0, itemCount - 1)
                valueJob?.cancel()
                valueJob = launch { valueAnimation.animateTo(nextTarget, valueAnimationSpec) }
                if (velocity != 0f) {
                    velocityJob?.cancel()
                    velocityJob = launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release(onSettled = onSettled)
            }
        }
    }

    /**
     * 处理拖拽事件（9.0.0 跟手逻辑 + KSU 压按形变）。
     *
     * 使用 snapTo 确保指示器位置完全跟手，
     * 同时通过 desiredValue 记录超滚状态，
     * 手势速度只用于释放投影；形变速度对齐 KSU，从指示器 value 轨迹平滑估算。
     */
    fun onDrag(
        dragAmountPx: Float,
        itemWidthPx: Float,
        gestureVelocityPxPerSecond: Float = 0f
    ) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        if (!isDragging) {
            isDragging = true
            startNewMotion()
            valueJob?.cancel()
            offsetJob?.cancel()
            desiredValue = valueAnimation.value
            velocityPxPerSecond = 0f
            velocityJob?.cancel()
            velocityJob = scope.launch { velocityAnimation.snapTo(0f) }
            press()
        }
        velocityPxPerSecond = gestureVelocityPxPerSecond

        // 9.0.0 风格：带阻力和超滚约束的期望位置跟踪
        val currentValue = desiredValue
        val isOverscrolling = currentValue < 0f || currentValue > (itemCount - 1).toFloat()
        val baseResistance = motionSpec.drag.baseResistance
        val overscrollResistance = motionSpec.drag.overscrollResistance

        val newDesiredValue = desiredValue + (dragAmountPx / itemWidthPx) *
            if (isOverscrolling) overscrollResistance else baseResistance
        desiredValue = newDesiredValue.fastCoerceIn(
            -motionSpec.drag.overscrollLimitItems,
            (itemCount - 1).toFloat() + motionSpec.drag.overscrollLimitItems
        )

        // 立即 snapTo 保证完全跟手（9.0.0 风格）
        val clampedValue = desiredValue.fastCoerceIn(0f, (itemCount - 1).toFloat())
        valueJob?.cancel()
        valueJob = scope.launch {
            valueAnimation.snapTo(clampedValue)
            updateDeformationVelocity(clampedValue)
        }

        // 面板偏移累计
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.snapTo(offsetAnimation.value + dragAmountPx)
        }
    }

    fun setPressed(pressed: Boolean) {
        if (pressed) {
            press()
        } else if (!isDragging) {
            release()
        }
    }

    /**
     * 处理拖拽结束（9.0.0 速度飞掷投影 + KSU 落位形变）。
     */
    fun onDragEnd(
        velocityX: Float,
        itemWidthPx: Float,
        settleIndex: Int? = null,
        notifyIndexChanged: Boolean = true
    ) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        isDragging = false
        val generation = motionGeneration
        velocityPxPerSecond = velocityX

        // 9.0.0 风格：速度飞掷投影确定吸附目标
        val releaseTargetIndex = settleIndex?.coerceIn(0, itemCount - 1)
            ?: resolveDampedDragReleaseTargetIndex(
                currentValue = desiredValue,
                velocityPxPerSecond = velocityX,
                itemWidthPx = itemWidthPx,
                itemCount = itemCount,
                motionSpec = motionSpec
            )
        targetIndex = releaseTargetIndex
        desiredValue = releaseTargetIndex.toFloat()
        if (notifyIndexChanged && notifyIndexChangedOnReleaseStart) {
            onIndexChanged(releaseTargetIndex)
        }
        animateToValue(releaseTargetIndex.toFloat()) {
            if (generation == motionGeneration) {
                velocityPxPerSecond = 0f
                settledReleaseCount += 1
                if (notifyIndexChanged && !notifyIndexChangedOnReleaseStart) {
                    onIndexChanged(releaseTargetIndex)
                }
            }
        }
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.animateTo(0f, offsetSnapAnimationSpec)
        }
    }

    fun updateIndex(index: Int) {
        if (isDragging || itemCount <= 0) return
        val safeIndex = index.coerceIn(0, itemCount - 1)
        if (
            safeIndex == targetIndex &&
            (
                isRunning ||
                    abs(value - safeIndex.toFloat()) < 0.005f ||
                    abs(targetValue - safeIndex.toFloat()) < 0.005f
                )
        ) return
        startNewMotion()
        targetIndex = safeIndex
        desiredValue = safeIndex.toFloat()
        velocityPxPerSecond = 0f
        animateToValue(safeIndex.toFloat()) {
            settledSelectionCount += 1
        }
    }
}

private const val KERNEL_SU_PRESSED_SCALE = 78f / 56f

@Composable
internal fun rememberDampedDragAnimationState(
    initialIndex: Int,
    itemCount: Int,
    onIndexChanged: (Int) -> Unit,
    motionSpec: BottomBarMotionSpec = resolveBottomBarMotionSpec(),
    notifyIndexChangedOnReleaseStart: Boolean = false,
    holdPressUntilReleaseTargetSettles: Boolean = false
): DampedDragAnimationState {
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)

    return remember(
        itemCount,
        motionSpec,
        notifyIndexChangedOnReleaseStart,
        holdPressUntilReleaseTargetSettles
    ) {
        DampedDragAnimationState(
            initialIndex = initialIndex,
            itemCount = itemCount,
            scope = scope,
            onIndexChanged = { currentOnIndexChanged(it) },
            motionSpec = motionSpec,
            notifyIndexChangedOnReleaseStart = notifyIndexChangedOnReleaseStart,
            holdPressUntilReleaseTargetSettles = holdPressUntilReleaseTargetSettles
        )
    }
}

/**
 * 水平拖拽手势 Modifier（9.0.0 发行版实现，带速度追踪）。
 *
 * 使用标准 Compose Foundation 手势 API 确保可靠的触摸检测：
 * - awaitFirstDown(requireUnconsumed = false)：保证被点击层消费后仍能触发拖拽
 * - awaitHorizontalTouchSlopOrCancellation：等待触摸斜率阈值后才激活拖拽（避免误触为点击）
 * - horizontalDrag：标准的 Compose 拖拽事件循环
 * - VelocityTracker：跟踪帧间速度用于飞掷投影和形变
 */
internal fun Modifier.horizontalDragGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float,
    consumePointerChanges: Boolean = true,
    settleIndex: Int? = null,
    notifyIndexChanged: Boolean = true
): Modifier = this.pointerInput(
    dragState,
    itemWidthPx,
    consumePointerChanges,
    settleIndex,
    notifyIndexChanged
) {
    awaitEachGesture {
        val velocityTracker = VelocityTracker()
        val down = awaitFirstDown(requireUnconsumed = false)
        velocityTracker.resetTracking()
        velocityTracker.addPosition(down.uptimeMillis, down.position)

        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
            if (consumePointerChanges) {
                change.consume()
            }
            dragState.onDrag(over, itemWidthPx)
        }

        if (dragStart != null) {
            velocityTracker.addPosition(dragStart.uptimeMillis, dragStart.position)
            var isCanceled = false

            try {
                horizontalDrag(dragStart.id) { change ->
                    if (consumePointerChanges) {
                        change.consume()
                    }
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val dragAmount = change.position.x - change.previousPosition.x
                    val velocity = velocityTracker.calculateVelocity()
                    dragState.onDrag(dragAmount, itemWidthPx, velocity.x)
                }
            } catch (_: Exception) {
                isCanceled = true
            }

            if (!isCanceled) {
                val velocity = velocityTracker.calculateVelocity()
                dragState.onDragEnd(
                    velocityX = velocity.x,
                    itemWidthPx = itemWidthPx,
                    settleIndex = settleIndex,
                    notifyIndexChanged = notifyIndexChanged
                )
            } else {
                dragState.onDragEnd(
                    velocityX = 0f,
                    itemWidthPx = itemWidthPx,
                    settleIndex = settleIndex,
                    notifyIndexChanged = notifyIndexChanged
                )
            }
        }
    }
}
