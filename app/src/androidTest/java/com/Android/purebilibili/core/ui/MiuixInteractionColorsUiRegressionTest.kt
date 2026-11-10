package com.android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class MiuixInteractionColorsUiRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun onlyOpaqueMiuixUsesNativeRolesEvenUnderLocalMaterialOverrides() {
        val style = mutableStateOf(AppUiStyle.MIUIX)
        val glass = mutableStateOf(false)
        var navigation: AppNavigationStateColors? = null
        var follow: AppActionStateColors? = null
        var followed: AppActionStateColors? = null
        var disabled: AppActionStateColors? = null
        var expectedFollow: AppActionStateColors? = null
        var expectedFollowed: AppActionStateColors? = null
        var expectedDisabled: AppActionStateColors? = null
        var actualOnAccent = Color.Unspecified
        var expectedOnAccent = Color.Unspecified
        rule.setContent {
            CompositionLocalProvider(
                LocalAppUiStyle provides style.value,
                LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
            ) {
                MaterialTheme {
                    MiuixTheme {
                        val rootGlass = glass.value
                        ProvideAppContentColors(nativeInteractionColors = style.value == AppUiStyle.MIUIX && !rootGlass) {
                            // 页面为展示原生菜单局部关闭玻璃，不得改变根主题的颜色边界。
                            CompositionLocalProvider(LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = false)) {
                                val nav = AppInteractionColors.nativeNavigation()
                                val primary = AppInteractionColors.nativeFollow(false)
                                val secondary = AppInteractionColors.nativeFollow(true)
                                val inactive = AppInteractionColors.nativeFollow(false, enabled = false)
                                val primaryDefaults = ButtonDefaults.buttonColors()
                                val secondaryDefaults = ButtonDefaults.buttonColors()
                                val foreground = AppContentColors.onAccent()
                                val expected = if (style.value == AppUiStyle.MIUIX && !rootGlass) {
                                    MiuixTheme.colorScheme.onPrimary
                                } else MaterialTheme.colorScheme.onPrimary
                                SideEffect {
                                    navigation = nav
                                    follow = primary
                                    followed = secondary
                                    disabled = inactive
                                    expectedFollow = AppActionStateColors(primaryDefaults.color, primaryDefaults.contentColor)
                                    expectedFollowed = AppActionStateColors(secondaryDefaults.color, secondaryDefaults.contentColor)
                                    expectedDisabled = AppActionStateColors(primaryDefaults.disabledColor, primaryDefaults.disabledContentColor)
                                    actualOnAccent = foreground
                                    expectedOnAccent = expected
                                }
                            }
                        }
                    }
                }
            }
        }
        for (theme in AppUiStyle.entries) {
            for (glassEnabled in listOf(false, true)) {
                rule.runOnIdle { style.value = theme; glass.value = glassEnabled }
                rule.runOnIdle {
                    assertEquals(expectedOnAccent, actualOnAccent)
                    if (theme == AppUiStyle.MIUIX && !glassEnabled) {
                        assertEquals(expectedFollow, follow)
                        assertEquals(expectedFollowed, followed)
                        assertEquals("关系状态变化不应引入主题色", follow, followed)
                        assertEquals(expectedDisabled, disabled)
                        assertNotEquals(navigation?.selectedContent, navigation?.unselectedContent)
                    } else {
                        assertNull(navigation)
                        assertNull(follow)
                        assertNull(followed)
                        assertNull(disabled)
                    }
                }
            }
        }
    }
}
