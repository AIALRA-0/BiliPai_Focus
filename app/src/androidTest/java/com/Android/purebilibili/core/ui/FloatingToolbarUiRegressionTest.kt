package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.components.AppFloatingToolbar
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class FloatingToolbarUiRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun material3ActionsRemainReachableAfterWindowAndFontChanges() = verify(AppUiStyle.MATERIAL3)

    @Test fun miuixActionsRemainReachableAfterWindowAndFontChanges() = verify(AppUiStyle.MIUIX)

    private fun verify(style: AppUiStyle) {
        val width = mutableStateOf(360)
        val fontScale = mutableStateOf(1f)
        var invoked = ""
        val labels = listOf("全选", "复制", "移动", "移至已看完", "取消选择")
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalAppUiStyle provides style,
                LocalDensity provides Density(density, fontScale.value),
            ) {
                MaterialTheme { MiuixTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.width(width.value.dp).testTag("viewport")) {
                            AppFloatingToolbar(Modifier.testTag("toolbar")) {
                                labels.forEach { label ->
                                    AppTextButton(onClick = { invoked = label }) { AppText(label) }
                                }
                            }
                        }
                    }
                } }
            }
        }

        for ((windowWidth, scale) in listOf(360 to 1f, 220 to 1.6f, 360 to 1f)) {
            rule.runOnIdle {
                width.value = windowWidth
                fontScale.value = scale
            }
            val viewport = rule.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
            val toolbar = rule.onNodeWithTag("toolbar").fetchSemanticsNode().boundsInRoot
            assertTrue("Toolbar must stay inside the window", toolbar.left >= viewport.left && toolbar.right <= viewport.right)
            // Both ends must remain actionable after overflow and after resizing back.
            for (label in listOf(labels.last(), labels.first())) {
                rule.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
                rule.runOnIdle { assertEquals(label, invoked) }
            }
        }
    }
}
