package com.Android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DanmakuSettingsPanelUiRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dismissingSettingsFromBackdropDoesNotClickUnderlyingRelatedVideo() {
        val showSettings = mutableStateOf(false)
        val relatedTitle = "相关视频"
        var relatedClicks = 0
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.size(width = 390.dp, height = 844.dp)) {
                    Button(
                        onClick = { relatedClicks++ },
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) { Text(relatedTitle) }
                    Button(
                        onClick = { showSettings.value = true },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) { Text("弹幕设置") }
                    if (showSettings.value) {
                        // The panel now owns its modal window; VideoContentSection no longer hosts it.
                        DanmakuSettingsPanel(
                            isFullscreen = false,
                            opacity = 1f,
                            fontScale = 1f,
                            speed = 1f,
                            onOpacityChange = {},
                            onFontScaleChange = {},
                            onSpeedChange = {},
                            onDismiss = { showSettings.value = false },
                        )
                    }
                }
            }
        }
        val relatedCenter = composeTestRule.onNodeWithText(relatedTitle)
            .fetchSemanticsNode().boundsInRoot.center
        composeTestRule.onNodeWithText("弹幕设置").performClick()
        composeTestRule.onNode(isDialog()).performTouchInput { click(relatedCenter) }
        composeTestRule.waitUntil(timeoutMillis = 2_000) { !showSettings.value }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(0, relatedClicks) }

        composeTestRule.onNodeWithText(relatedTitle).performClick()
        composeTestRule.runOnIdle { assertEquals(1, relatedClicks) }
    }

    @Test
    fun selectedBlockTypeChipActuallyDisablesAndRestoresScrollingDanmaku() {
        val allowScroll = mutableStateOf(true)
        composeTestRule.setContent {
            MaterialTheme {
                DanmakuSettingsPanel(
                    isFullscreen = false,
                    opacity = 1f,
                    fontScale = 1f,
                    speed = 1f,
                    allowScroll = allowScroll.value,
                    onOpacityChange = {},
                    onFontScaleChange = {},
                    onSpeedChange = {},
                    onAllowScrollChange = { allowScroll.value = it },
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("滚动").assertIsNotSelected().performClick()
        composeTestRule.runOnIdle { assertEquals(false, allowScroll.value) }
        composeTestRule.onNodeWithText("滚动").assertIsSelected().performClick()
        composeTestRule.runOnIdle { assertEquals(true, allowScroll.value) }
        composeTestRule.onNodeWithText("滚动").assertIsNotSelected()
    }
}
