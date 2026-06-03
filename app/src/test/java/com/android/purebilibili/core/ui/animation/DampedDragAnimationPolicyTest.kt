package com.android.purebilibili.core.ui.animation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DampedDragAnimationPolicyTest {

    @Test
    fun `velocity conversion guards invalid item width`() {
        assertEquals(
            0f,
            resolveDampedDragVelocityItemsPerSecond(
                velocityPxPerSecond = 1200f,
                itemWidthPx = 0f
            )
        )
    }

    @Test
    fun `shared drag animation uses 9-0-0 gesture detection with KSU drag scale`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val dragSource = source
            .substringAfter("fun onDrag(")
            .substringBefore("fun onDragEnd(")
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun updateIndex(")

        // KSU 拖动手感保留
        assertTrue(source.contains("private const val KERNEL_SU_PRESSED_SCALE = 78f / 56f"))
        assertTrue(source.contains("private val valueAnimationSpec = spring(1f, 1000f, 0.001f)"))
        assertTrue(source.contains("private val velocityAnimationSpec = spring(0.5f, 300f, 0.01f)"))
        // 9.0.0 标准手势检测 API
        assertTrue(source.contains("awaitEachGesture"))
        assertTrue(source.contains("awaitFirstDown(requireUnconsumed = false)"))
        assertTrue(source.contains("awaitHorizontalTouchSlopOrCancellation"))
        assertTrue(source.contains("horizontalDrag(dragStart.id)"))
        // 9.0.0 跟手逻辑：snapTo + desiredValue + 阻力超滚
        assertTrue(dragSource.contains("valueAnimation.snapTo(clampedValue)"))
        assertTrue(dragSource.contains("desiredValue ="))
        assertTrue(dragSource.contains("motionSpec.drag.baseResistance"))
        assertTrue(dragSource.contains("overscrollResistance"))
        assertTrue(dragSource.contains("overscrollLimitItems"))
        // 偏移累计保留（KSU 风格）
        assertTrue(dragSource.contains("offsetAnimation.snapTo(offsetAnimation.value + dragAmountPx)"))
        // 9.0.0 速度飞掷投影
        assertTrue(releaseSource.contains("resolveDampedDragReleaseTargetIndex("))
        assertTrue(releaseSource.contains("velocityPxPerSecond = velocityX"))
        assertTrue(releaseSource.contains("offsetAnimation.animateTo(0f"))
    }

    @Test
    fun `drag velocity uses KernelSU value tracker for indicator deformation`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val dragSource = source
            .substringAfter("fun onDrag(")
            .substringBefore("fun setPressed(")

        assertTrue(source.contains("val deformationVelocityItemsPerSecond: Float get() = velocityAnimation.value"))
        assertTrue(source.contains("private val deformationVelocityTracker = VelocityTracker()"))
        assertTrue(source.contains("deformationVelocityTracker.resetTracking()"))
        assertTrue(source.contains("deformationVelocityTracker.addPosition("))
        assertTrue(source.contains("Offset(value, 0f)"))
        assertTrue(source.contains("velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)"))
        assertTrue(dragSource.contains("updateDeformationVelocity(clampedValue)"))
        assertTrue(source.contains("velocityTracker.addPosition("))
        assertTrue(source.contains("velocityTracker.calculateVelocity()"))
        // velocityAnimation 仍通过 animateToValue 中的 animateTo(0f) 做释放衰减
        assertTrue(source.contains("velocityAnimation.animateTo(0f, velocityAnimationSpec)"))
        assertFalse(dragSource.contains("velocityAnimation.snapTo(gestureVelocityItems)"))
        assertFalse(source.contains("deformationVelocityAnimation"))
        assertFalse(source.contains("dragVelocityItemsPerSecond"))
    }

    @Test
    fun `settle pulse counters distinguish drag release and click selection`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun updateIndex(")
        val updateIndexSource = source
            .substringAfter("fun updateIndex(index: Int)")
            .substringBefore("private const val KERNEL_SU_PRESSED_SCALE")

        assertTrue(source.contains("var settledReleaseCount by mutableIntStateOf(0)"))
        assertTrue(source.contains("var settledSelectionCount by mutableIntStateOf(0)"))
        assertTrue(releaseSource.contains("settledReleaseCount += 1"))
        assertFalse(updateIndexSource.contains("settledReleaseCount += 1"))
        assertTrue(updateIndexSource.contains("settledSelectionCount += 1"))
        assertFalse(releaseSource.contains("settledSelectionCount += 1"))
    }

    @Test
    fun `click index update keeps press progress until target settles`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val updateIndexSource = source
            .substringAfter("fun updateIndex(index: Int)")
            .substringBefore("private const val KERNEL_SU_PRESSED_SCALE")

        assertTrue(updateIndexSource.contains("animateToValue(safeIndex.toFloat())"))
        assertTrue(source.contains("fun animateToValue(value: Float, onSettled: (() -> Unit)? = null)"))
        assertTrue(source.contains("press()"))
        assertTrue(source.contains("release(onSettled = onSettled)"))
    }

    @Test
    fun `bottom bar input layer keeps horizontal drag gesture wiring`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()
        val inputLayerSource = source
            .substringAfter("private fun BoxScope.KernelSuBottomBarInputLayer(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarSearchSlot(")

        assertTrue(inputLayerSource.contains(".horizontalDragGesture("))
        assertTrue(inputLayerSource.contains("dragState = dampedDragState"))
        assertTrue(inputLayerSource.contains("itemWidthPx = itemWidthPx"))
        assertTrue(inputLayerSource.contains("onPressChanged = dampedDragState::setPressed"))
        assertFalse(inputLayerSource.contains("pointerInput("))
    }
}
