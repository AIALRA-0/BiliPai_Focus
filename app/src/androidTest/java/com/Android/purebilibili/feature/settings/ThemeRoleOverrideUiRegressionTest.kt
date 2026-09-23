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
            .onNodeWithText("当前文字与背景对比度偏低，仍可按精确颜色保存。")
            .assertIsDisplayed()
    }
}
