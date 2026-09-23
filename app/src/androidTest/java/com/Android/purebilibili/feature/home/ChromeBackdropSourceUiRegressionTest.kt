package com.android.purebilibili.feature.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.blur.drawBackdrop

class ChromeBackdropSourceUiRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun visibleSourceUpdatesWithoutDrawingChildrenTwice() = verifySource()

    @Test
    fun hiddenDockCaptureStillUpdatesItsConsumer() = verifySource(hidden = true)

    @Test
    fun nestedNavigationAndHomeSourcesDoNotMultiplyChildDrawing() =
        verifySource(nested = true)

    private fun verifySource(hidden: Boolean = false, nested: Boolean = false) {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val tint = mutableStateOf(Color.Red)
        val drawCalls = AtomicInteger()
        composeRule.setContent {
            val source = rememberChromeBackdropSource()
            val innerSource = rememberChromeBackdropSource()
            Box(Modifier.size(80.dp).testTag("backdrop-result").background(Color.Blue)) {
                Box(
                    Modifier.matchParentSize()
                        .alpha(if (hidden) 0f else 1f)
                        .then(source.modifier)
                ) {
                    Box(
                        Modifier.matchParentSize()
                            .then(if (nested) innerSource.modifier else Modifier)
                            .drawBehind {
                                drawCalls.incrementAndGet()
                                drawRect(tint.value)
                            }
                    )
                }
                // No tint/blur: the consumer must reproduce the source, even when it is hidden.
                Box(
                    Modifier.matchParentSize().drawBackdrop(
                        backdrop = source.backdrop,
                        shape = { RectangleShape },
                        effects = {},
                    )
                )
            }
        }
        composeRule.waitForIdle()
        assertCenterColor(Color.Red)

        composeRule.runOnIdle {
            drawCalls.set(0)
            tint.value = Color.Green
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals("One child draw after a draw-only invalidation", 1, drawCalls.get())
        }
        assertCenterColor(Color.Green)
    }

    private fun assertCenterColor(expected: Color) {
        val image = composeRule.onNodeWithTag("backdrop-result").captureToImage()
        val pixel = image.toPixelMap()[image.width / 2, image.height / 2]
        assertEquals(expected.red, pixel.red, 0.01f)
        assertEquals(expected.green, pixel.green, 0.01f)
        assertEquals(expected.blue, pixel.blue, 0.01f)
        assertEquals(expected.alpha, pixel.alpha, 0.01f)
    }
}
