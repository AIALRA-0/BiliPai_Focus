package com.android.purebilibili.core.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.core.theme.AppFontSizePreset
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.PureBiliBiliTheme
import com.android.purebilibili.core.ui.common.TextSelectionBottomSheet
import com.android.purebilibili.feature.dynamic.components.DynamicInlineCommentHeader
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactContentLayoutUiRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun material3LongTextKeepsCopyActionsInShortDialog() = verifyCopyPanel(AppUiStyle.MATERIAL3)

    @Test fun miuixLongTextKeepsCopyActionsInShortDialog() = verifyCopyPanel(AppUiStyle.MIUIX)

    @Test fun material3CommentHeaderWrapsWithoutLosingActions() = verifyCommentHeader(AppUiStyle.MATERIAL3)

    @Test fun miuixCommentHeaderWrapsWithoutLosingActions() = verifyCommentHeader(AppUiStyle.MIUIX)

    private fun verifyCopyPanel(style: AppUiStyle) {
        val text = (1..60).joinToString("\n") { "第${it}段：长文本可以滚动，复制和分享操作必须保留。" }
        val visible = mutableStateOf(true)
        rule.setContent {
            // Exercise the centered, height-limited dialog independently of device orientation.
            val shortWindow = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = 914
                screenHeightDp = 360
            }
            CompositionLocalProvider(
                LocalConfiguration provides shortWindow,
                LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = false),
            ) {
                PureBiliBiliTheme(uiStyle = style, liquidGlassEnabled = false, themeTransitionEnabled = false) {
                    if (visible.value) {
                        TextSelectionBottomSheet(text, "选择动态内容") { visible.value = false }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("分享").assertIsDisplayed()
        rule.onNodeWithText("复制全部").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertFalse("Copy must dismiss the panel", visible.value)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(text, clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString())
        }
    }

    private fun verifyCommentHeader(style: AppUiStyle) {
        val width = mutableStateOf(320)
        val sort = mutableStateOf(CommentSortMode.HOT)
        var reactionOpens = 0
        rule.setContent {
            CompositionLocalProvider(LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = false)) {
                PureBiliBiliTheme(
                    uiStyle = style,
                    fontSizePreset = AppFontSizePreset.EXTRA_EXTRA_LARGE,
                    liquidGlassEnabled = false,
                    themeTransitionEnabled = false,
                ) {
                    Box(Modifier.width(width.value.dp).testTag("comment-window")) {
                        DynamicInlineCommentHeader(
                            totalCount = 9999,
                            sortMode = sort.value,
                            onSortModeChange = { sort.value = it },
                            onShowReactions = { reactionOpens++ },
                        )
                    }
                }
            }
        }
        for ((index, windowWidth) in listOf(320, 220, 320).withIndex()) {
            rule.runOnIdle {
                width.value = windowWidth
                sort.value = CommentSortMode.HOT
            }
            val viewport = rule.onNodeWithTag("comment-window").fetchSemanticsNode().boundsInRoot
            val count = rule.onNodeWithText("9999条回复").fetchSemanticsNode().boundsInRoot
            val reactions = rule.onNodeWithText("赞和转发 ›").fetchSemanticsNode().boundsInRoot
            // Include the control shell, not just its labels: it used to cover the count first.
            val control = rule.onAllNodes(
                hasAnyDescendant(hasText("最热")) and hasAnyDescendant(hasText("最新")),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().map { it.boundsInRoot }.minBy { it.width * it.height }
            assertTrue("Sort control must stay in the window", control.left >= viewport.left - 0.5f && control.right <= viewport.right + 0.5f)
            assertFalse("Sort control must not cover the reply count", control.overlaps(count))
            assertFalse("Sort control must not cover the reaction entry", control.overlaps(reactions))
            rule.onNodeWithText("最新").assertIsDisplayed().performClick()
            rule.onNodeWithText("赞和转发 ›").assertIsDisplayed().performClick()
            rule.runOnIdle {
                assertEquals(CommentSortMode.NEWEST, sort.value)
                assertEquals(index + 1, reactionOpens)
            }
        }
    }
}
