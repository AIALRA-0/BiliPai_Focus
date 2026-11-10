package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanelBody
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsSlider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DanmakuSettingsPanelBodyUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun completionRemainsVisibleAfterScrollingThroughLongSettings() {
        var dismissed = 0
        rule.setContent {
            MaterialTheme {
                DanmakuSettingsPanelBody(
                    onDismiss = { dismissed++ },
                    modifier = Modifier.size(280.dp, 220.dp),
                ) {
                    repeat(30) { index ->
                        Box(Modifier.fillMaxWidth().height(44.dp)) { AppText("设置 $index") }
                    }
                }
            }
        }
        rule.onNodeWithText("设置 29").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("弹幕设置").assertIsDisplayed()
        rule.onNodeWithText("完成").assertIsDisplayed().performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun incomingValuesDoNotSaveAndResetCommitsExactlyOnce() {
        val value = mutableStateOf(0.75f)
        var previews = 0
        var commits = 0
        rule.setContent {
            MaterialTheme {
                DanmakuSettingsSlider(
                    label = "显示区域",
                    valueText = { "${(it * 100).toInt()}%" },
                    value = value.value,
                    valueRange = 0.25f..1f,
                    steps = 2,
                    defaultValue = 0.5f,
                    onValuePreview = { previews++ },
                    onValueChangeFinished = { commits++; value.value = it },
                )
            }
        }
        rule.runOnIdle { value.value = 1f }
        rule.onNodeWithText("100%").assertIsDisplayed()
        assertEquals(0, commits)
        rule.onNodeWithContentDescription("恢复显示区域默认值 50%").performClick()
        rule.runOnIdle {
            assertEquals(0.5f, value.value)
            assertEquals(1, previews)
            assertEquals(1, commits)
        }
        rule.onNodeWithContentDescription("恢复显示区域默认值 50%").assertIsNotEnabled()
    }
}
