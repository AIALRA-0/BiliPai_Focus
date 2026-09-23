package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.calculateContrastRatio
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomInputBarStructureTest {

    @Test
    fun bottomInputBar_keepsSolidDockedPathWhenLiquidReuseAndBlurAreOff() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.surface"))
        assertTrue(source.contains("MaterialTheme.colorScheme.surfaceContainerHighest"))
        assertTrue(source.contains("DockedSolidBottomInputBar("))
        assertTrue(!source.contains("surfaceVariant.copy(alpha = 0.65f)"))
        assertTrue(!source.contains("liquidGlassBackground"))
    }

    @Test
    fun bottomInputBar_reusesHomeFloatingLiquidDockWhenReuseEnabled() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("shouldUseFloatingLiquidBottomInputBar("))
        assertTrue(source.contains("resolveGlobalLiquidGlassReuseEnabled"))
        assertTrue(source.contains("FloatingLiquidBottomInputBar("))
        assertTrue(source.contains("text = \"写评论\""))
        assertTrue(source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(source.contains("reuseEnabled = true"))
        // 输入区和操作区保持两个独立液态 Dock，并分别进行安全缩放后的 lens 渲染。
        assertEquals(2, source.substringAfter("private fun FloatingLiquidBottomInputBar(")
            .substringBefore("private fun BottomInputBarContentRow(")
            .split("BottomBarMatchedReusableLiquidDock(").size - 1)
        assertTrue(source.contains("drawShellLens = true"))
        assertFalse(source.contains("drawShellLens = false"))
        assertEquals(2, source.substringAfter("private fun FloatingLiquidBottomInputBar(")
            .substringBefore("private fun BottomInputBarContentRow(")
            .split("shellLensIntensity = resolveFloatingDockGeometryScale(44f)").size - 1)
        assertTrue(source.contains("黑虾线防回归"))
        assertTrue(source.contains("不能合成长壳"))
        assertTrue(source.contains("直接关 lens 会损失液态玻璃"))
        assertTrue(source.contains("itemSize = 32.dp"))
        assertTrue(source.contains("iconSize = 19.dp"))
        assertTrue(source.contains("spreadItems = true"))
        assertTrue(source.contains("itemSize: Dp = 48.dp"))
        assertTrue(source.contains("Arrangement.spacedBy(8.dp)"))
        assertTrue(source.contains("发一条友善的评论…"))
        assertFalse(source.contains("BottomBarMatchedLiquidDock("))
        assertTrue(!source.contains(".biliPaiFloatingDockSurface("))
        assertTrue(source.contains("resolveSharedBottomBarCapsuleShape()"))
        assertTrue(!source.contains("resolveAndroidNativeFloatingBottomBarContainerColor("))
        assertTrue(!source.contains("commentFieldContainerColor"))
        assertTrue(source.contains("backdrop: Backdrop? = null"))
    }

    @Test
    fun floatingCommentBar_keepsGestureAreaTransparentAndCapsulesInset() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()
        val floatingBar = source.substringAfter("private fun FloatingLiquidBottomInputBar(")
            .substringBefore("private fun BottomInputBarContentRow(")

        assertFalse(floatingBar.contains(".height(navigationBarBottomPadding)"))
        assertFalse(floatingBar.contains(".background("))
        assertTrue(floatingBar.contains("val bottomInset = 12.dp + navigationBarBottomPadding"))
        assertTrue(floatingBar.contains(".padding(bottom = bottomInset)"))
        // The non-floating bar still owns its full-width surface and safe-area padding.
        val dockedBar = source.substringAfter("private fun DockedSolidBottomInputBar(")
            .substringBefore("private fun FloatingLiquidBottomInputBar(")
        assertTrue(dockedBar.contains(".navigationBarsPadding()"))
    }

    @Test
    fun floatingLiquidGate_followsGlobalReuseMasterOnly() {
        assertTrue(shouldUseFloatingLiquidBottomInputBar(androidNativeLiquidGlassEnabled = true))
        assertFalse(shouldUseFloatingLiquidBottomInputBar(androidNativeLiquidGlassEnabled = false))
    }

    @Test
    fun frostedCommentBar_followsBottomBarBlurPreferenceWhenLiquidGlassIsOff() {
        assertTrue(
            shouldUseFrostedBottomInputBar(
                bottomBarBlurEnabled = true,
                floatingLiquidGlass = false,
                hasHazeState = true
            )
        )
        assertFalse(
            shouldUseFrostedBottomInputBar(
                bottomBarBlurEnabled = false,
                floatingLiquidGlass = false,
                hasHazeState = true
            )
        )
        assertFalse(
            shouldUseFrostedBottomInputBar(
                bottomBarBlurEnabled = true,
                floatingLiquidGlass = true,
                hasHazeState = true
            )
        )
        assertFalse(
            shouldUseFrostedBottomInputBar(
                bottomBarBlurEnabled = true,
                floatingLiquidGlass = false,
                hasHazeState = false
            )
        )
    }

    @Test
    fun frostedCommentBar_reusesHomeBottomBarBlurPipeline() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("resolveBottomBarSurfaceColor("))
        assertTrue(source.contains("Modifier.unifiedBlur("))
        assertTrue(source.contains("surfaceType = BlurSurfaceType.BOTTOM_BAR"))
        assertFalse(source.contains("HazeMaterials.ultraThin()"))
    }

    @Test
    fun contentBottomPadding_growsWhenFloatingLiquidChrome() {
        assertEquals(
            112.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = true,
                floatingLiquidGlass = true,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            96.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = true,
                floatingLiquidGlass = false,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            84.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = false,
                floatingLiquidGlass = true,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            12.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = false,
                floatingLiquidGlass = false,
                showActionButtonsFallback = false
            )
        )
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInLightTheme() {
        val inputContainerColor = Color(0xFFE6E0E9)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFF1D1B20),
            onSurfaceVariantColor = Color(0xFF49454F)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInDarkTheme() {
        val inputContainerColor = Color(0xFF36343B)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFFE6E1E5),
            onSurfaceVariantColor = Color(0xFFCAC4D0)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }
}
