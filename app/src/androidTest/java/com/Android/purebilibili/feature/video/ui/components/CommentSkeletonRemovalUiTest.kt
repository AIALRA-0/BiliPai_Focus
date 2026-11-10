package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.feature.dynamic.components.dynamicCommentSkeletonItems
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CommentSkeletonRemovalUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dynamicSkeletonLeavesNoPixelsBelowLoadedContentWithoutScrolling() =
        assertSkeletonRemoved(dynamic = true)

    @Test
    fun threadSkeletonLeavesNoPixelsBelowLoadedContentWithoutScrolling() =
        assertSkeletonRemoved(dynamic = false)

    private fun assertSkeletonRemoved(dynamic: Boolean) {
        val loading = mutableStateOf(true)
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppThemeConfig provides AppThemeConfig(uiEntranceAnimationEnabled = true),
                ) {
                    LazyColumn(
                        modifier = Modifier.size(300.dp, 480.dp).background(Color.White).testTag("comments"),
                    ) {
                        if (loading.value) {
                            if (dynamic) dynamicCommentSkeletonItems(6, "loading_")
                            else videoCommentSkeletonItems(6, "loading_")
                        } else {
                            item(key = "loaded") {
                                Text("loaded comment", modifier = Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle { loading.value = false }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        // 检查像素而非仅检查语义节点；已删除节点的残留图层也必须消失。
        val pixels = rule.onNodeWithTag("comments").captureToImage().toPixelMap()
        for (y in pixels.height / 3 until pixels.height step 5) {
            for (x in 0 until pixels.width step 5) {
                val pixel = pixels[x, y]
                assertEquals("red at $x,$y", 1f, pixel.red, 0.01f)
                assertEquals("green at $x,$y", 1f, pixel.green, 0.01f)
                assertEquals("blue at $x,$y", 1f, pixel.blue, 0.01f)
            }
        }
    }
}
