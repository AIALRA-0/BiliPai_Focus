package com.android.purebilibili.feature.video

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.components.DanmakuSendDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DanmakuComposerUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compactComposer_focusesInputAndKeepsAdvancedSettingsCollapsed() {
        composeTestRule.setContent {
            MaterialTheme {
                DanmakuSendDialog(
                    visible = true,
                    initialText = "未发送草稿",
                    onDismiss = {},
                    onSend = { _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("danmaku_compact_input").assertIsFocused()
        composeTestRule.onNodeWithText("未发送草稿").assertIsDisplayed()
        composeTestRule.onNodeWithText("颜色").assertDoesNotExist()

        composeTestRule.onNodeWithText("颜色、位置与大小").performClick()
        composeTestRule.onNodeWithText("颜色").assertIsDisplayed()
    }
}
