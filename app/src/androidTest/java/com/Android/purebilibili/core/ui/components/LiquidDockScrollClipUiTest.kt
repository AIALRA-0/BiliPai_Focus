package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LiquidDockScrollClipUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun contentStaysInsideHorizontalEndsWhileIndicatorCanExpandVertically() {
        val translation = mutableFloatStateOf(0f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(140.dp, 100.dp).background(Color.Blue).testTag("viewport")) {
                    Box(
                        Modifier.offset(20.dp, 20.dp).size(100.dp, 40.dp)
                            .clipLiquidDockScrollContent(
                                leadingInsetPx = { 10f },
                                trailingInsetPx = { 5f },
                                translationYPx = { translation.floatValue },
                                verticalOverflowPx = 12f,
                            )
                            .drawBehind {
                                // 模拟滚动指示器及文字超出左右、上下边界。
                                drawRect(Color.Red, Offset(-20f, -20f), Size(140f, 80f))
                            },
                    )
                }
            }
        }
        assertPixel(15, 40, Color.Blue)
        assertPixel(25, 40, Color.Blue)
        assertPixel(118, 40, Color.Blue)
        assertPixel(70, 10, Color.Red)
        assertPixel(70, 6, Color.Blue)
        assertPixel(70, 70, Color.Red)
        assertPixel(70, 74, Color.Blue)
        assertPixel(70, 22, Color.Red)
        assertPixel(70, 40, Color.Red)

        rule.runOnIdle { translation.floatValue = 8f }
        assertPixel(70, 14, Color.Blue)
        assertPixel(70, 18, Color.Red)
        assertPixel(70, 30, Color.Red)
        assertPixel(70, 65, Color.Red)
        assertPixel(70, 78, Color.Red)
        assertPixel(70, 82, Color.Blue)
    }

    private fun assertPixel(x: Int, y: Int, expected: Color) {
        rule.waitForIdle()
        val actual = rule.onNodeWithTag("viewport").captureToImage().toPixelMap()[x, y]
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }
}
