package com.Android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.ProvideAppThemeConfig
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppWindowAction
import com.android.purebilibili.core.ui.components.AppWindowActionMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class AppWindowActionMenuUiRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nativeMenu_switchesLayoutOnce_andReopensWithUpdatedAction() {
        verifyLayoutSwitch(nativeMenu = true)
    }

    @Test
    fun materialMenu_switchesLayoutOnce_andReopensWithUpdatedAction() {
        verifyLayoutSwitch(nativeMenu = false)
    }

    private fun verifyLayoutSwitch(nativeMenu: Boolean) {
        var clicks = 0
        composeTestRule.setContent {
            ProvideAppThemeConfig(AppThemeConfig(nativeMiuixPopupsEnabled = nativeMenu)) {
                MiuixTheme {
                    MaterialTheme {
                        var singleColumn by remember { mutableStateOf(false) }
                        AppWindowActionMenu(
                            groups = listOf(listOf(AppWindowAction(
                                label = if (singleColumn) "切换为双列" else "切换为单列",
                                onClick = {
                                    clicks++
                                    singleColumn = !singleColumn
                                },
                            ))),
                        ) { AppText("更多") }
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("更多").performClick()
        composeTestRule.onNodeWithText("切换为单列").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicks) }
        composeTestRule.onNodeWithText("更多").performClick()
        composeTestRule.onNodeWithText("切换为双列").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertEquals(2, clicks) }
        composeTestRule.onNodeWithText("更多").performClick()
        composeTestRule.onNodeWithText("切换为单列").assertIsDisplayed()
    }

    @Test
    fun nativeMenu_opensSortOptions_returns_andDispatchesSelectedLeafOnce() {
        var selectedSort = "最新"
        var sortClicks = 0
        composeTestRule.setContent {
            MiuixTheme {
                MaterialTheme {
                    AppWindowActionMenu(
                        groups = listOf(
                            listOf(AppWindowAction(
                                label = "排序",
                                children = listOf(
                                    AppWindowAction(label = "最新", selected = true),
                                    AppWindowAction(label = "最热", onClick = {
                                        selectedSort = "最热"
                                        sortClicks++
                                    }),
                                ),
                            )),
                            listOf(AppWindowAction(label = "不可用", enabled = false)),
                        ),
                    ) { AppText("更多") }
                }
            }
        }
        composeTestRule.onNodeWithText("更多").performClick()
        composeTestRule.onNodeWithText("不可用").assertIsNotEnabled()
        composeTestRule.onNodeWithText("排序").performClick()
        composeTestRule.onNodeWithText("最热").assertIsDisplayed()
        composeTestRule.onNodeWithText("返回").performClick()
        composeTestRule.onNodeWithText("排序").performClick()
        composeTestRule.onNodeWithText("最热").performClick()
        composeTestRule.runOnIdle {
            assertEquals("最热", selectedSort)
            assertEquals(1, sortClicks)
        }
        composeTestRule.onNodeWithText("更多").performClick()
        composeTestRule.onNodeWithText("排序").assertIsDisplayed()
    }
}
