package com.android.purebilibili.feature.home

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundFrame
import com.android.purebilibili.core.ui.transition.videoCardTransitionSnapshotBlur
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ContainerSnapshotBlurUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun radiusAndRasterChangesReuseTheSourceAndReturnRefreshesItOnce() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val radius = mutableFloatStateOf(12f)
        val refresh = mutableStateOf(false)
        val color = mutableStateOf(Color.Red)
        val sourceDraws = AtomicInteger()
        rule.setContent {
            Box(Modifier.size(80.dp).testTag("snapshot")
                .videoCardTransitionSnapshotBlur(
                    frameProvider = {
                        if (radius.floatValue > 0f) VideoCardTransitionBackgroundFrame(
                            blurRadiusPx = radius.floatValue, scrimAlpha = 0f, contentScale = 1f,
                        ) else null
                    },
                    refreshSourceProvider = { refresh.value },
                )
                .drawWithContent {
                    sourceDraws.incrementAndGet()
                    drawRect(color.value)
                })
        }
        fun assertColor(expected: Color) {
            val pixels = rule.onNodeWithTag("snapshot").captureToImage().toPixelMap()
            val actual = pixels[pixels.width / 2, pixels.height / 2]
            assertEquals(expected.red, actual.red, 0.03f)
            assertEquals(expected.blue, actual.blue, 0.03f)
        }
        assertColor(Color.Red)
        rule.runOnIdle { sourceDraws.set(0) }
        for (nextRadius in listOf(18f, 2f, 14f)) {
            rule.runOnIdle { radius.floatValue = nextRadius }
            assertColor(Color.Red)
            rule.runOnIdle { assertEquals(0, sourceDraws.get()) }
        }
        rule.runOnIdle {
            color.value = Color.Blue
            refresh.value = true
        }
        assertColor(Color.Blue)
        rule.runOnIdle { assertEquals(1, sourceDraws.get()); sourceDraws.set(0) }
        rule.runOnIdle { radius.floatValue = 16f }
        assertColor(Color.Blue)
        rule.runOnIdle { assertEquals(0, sourceDraws.get()) }
        // 取消返回回到遮挡态后，再次返回仍须接回最新来源。
        rule.runOnIdle { refresh.value = false }
        assertColor(Color.Blue)
        rule.runOnIdle { refresh.value = true; color.value = Color.Red }
        assertColor(Color.Red)
        rule.runOnIdle { radius.floatValue = 0f; color.value = Color.Green }
        assertColor(Color.Green)
        rule.runOnIdle { radius.floatValue = 12f }
        assertColor(Color.Green)
    }
}
