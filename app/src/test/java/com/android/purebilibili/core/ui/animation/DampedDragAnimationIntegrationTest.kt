package com.android.purebilibili.core.ui.animation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DampedDragAnimationIntegrationTest {

    @Test
    fun `FloatingBottomBar wires drag modifiers on the moving indicator`() {
        val floating = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"),
        ).first { it.exists() }.readText()
        val dragPort = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/components/miuix/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/components/miuix/DampedDragAnimation.kt"),
        ).first { it.exists() }.readText()
        val body = floating.substringAfter("fun FloatingBottomBar(")
        val baseRow = body
            .substringAfter("CompositionLocalProvider(LocalFloatingBottomBarContentColor provides colors.contentColor)")
            .substringBefore("if (isLiquidGlassMode && backdrop != null)")
        val movingIndicator = body.substringAfter("if (tabWidthPx > 0f)")

        assertTrue(body.contains("DampedDragAnimation("))
        assertTrue(baseRow.contains(".then(dampedDragAnimation.modifier)"))
        assertTrue(baseRow.contains("interactiveHighlight.gestureModifier"))
        assertFalse(movingIndicator.contains(".then(dampedDragAnimation.modifier)"))
        assertFalse(movingIndicator.contains("interactiveHighlight?.gestureModifier"))
        assertTrue(body.contains("canDrag = { position ->"))
        assertTrue(body.contains("pressedScale = pressedScale"))
        assertTrue(dragPort.contains("inspectDragGestures("))
        assertTrue(dragPort.contains("val modifier: Modifier = Modifier.pointerInput(Unit)"))

        // Must not use design-system horizontal drag stack for the floating dock.
        assertFalse(floating.contains("horizontalDragGesture"))
        assertFalse(floating.contains("rememberDampedDragAnimationState"))
        assertFalse(floating.contains("BiliPaiBottomBarInputLayer"))
    }
}
