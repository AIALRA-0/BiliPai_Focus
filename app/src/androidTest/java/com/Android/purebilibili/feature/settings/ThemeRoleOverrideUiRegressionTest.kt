package com.android.purebilibili.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.store.ThemeModeRoleOverrides
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeRoleOverrideUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun roleEditor_showsExactColorsContrastAndWarning() {
        composeTestRule.setContent {
            MaterialTheme {
                ThemeRoleModeEditor(
                    title = "浅色模式",
                    roles = ThemeModeRoleOverrides(
                        backgroundHex = "#FFFFFF",
                        primaryTextHex = "#EEEEEE",
                        secondaryTextHex = "#F2F2F2",
                        controlAccentHex = "#0061A4"
                    ),
                    targets = ThemeRoleColorTarget.entries.take(4),
                    onColorClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("#FFFFFF").assertIsDisplayed()
        composeTestRule.onNode(hasText("对比度：", substring = true)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("你选的文字色与背景比较接近，预览已自动调整文字，让它更容易看清；仍可保存。")
            .assertIsDisplayed()
    }
    @Test
    fun explicitReadabilityRepairInvokesTheSaveCallback() {
        var repaired = false
        composeTestRule.setContent {
            MaterialTheme {
                ThemeRoleModeEditor(
                    title = "浅色模式",
                    roles = ThemeModeRoleOverrides("#FFFFFF", "#EEEEEE", "#F2F2F2", "#34C759"),
                    targets = emptyList(),
                    onColorClick = {},
                    onRepair = { repaired = true },
                    repairAvailable = true,
                )
            }
        }
        composeTestRule.onNodeWithText("修正可读性").assertIsEnabled().performClick()
        org.junit.Assert.assertTrue(repaired)
    }

}
