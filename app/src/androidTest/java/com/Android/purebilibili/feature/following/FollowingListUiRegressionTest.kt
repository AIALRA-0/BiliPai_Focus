package com.Android.purebilibili.feature.following

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.feature.following.FollowingListScreen
import com.android.purebilibili.feature.following.FollowingListUiState
import com.android.purebilibili.feature.following.FollowingListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class FollowingListUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun materialLargeFont_batchActionsDoNotCoverHeaderOrLastUser() {
        assertBatchLayout(AppUiStyle.MATERIAL3, glass = false)
    }

    @Test
    fun miuixLargeFont_batchActionsDoNotCoverHeaderOrLastUser() {
        assertBatchLayout(AppUiStyle.MIUIX, glass = false)
    }

    @Test
    fun materialGlassLargeFont_batchActionsDoNotCoverHeaderOrLastUser() {
        assertBatchLayout(AppUiStyle.MATERIAL3, glass = true)
    }

    @Test
    fun miuixGlassLargeFont_batchActionsDoNotCoverHeaderOrLastUser() {
        assertBatchLayout(AppUiStyle.MIUIX, glass = true)
    }

    private fun assertBatchLayout(style: AppUiStyle, glass: Boolean) {
        val viewModel = FollowingListViewModel()
        // Seed the real screen without adding a production-only test API or making requests.
        val field = FollowingListViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<FollowingListUiState>
        state.value = FollowingListUiState.Success(
            users = (1..12).map { FollowingUser(mid = it.toLong(), uname = "关注测试 $it") },
            total = 12,
            hasMore = false,
        )
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = 1.6f),
                LocalAppUiStyle provides style,
                LocalAppThemeConfig provides AppThemeConfig(
                    liquidGlassEnabled = glass,
                    headerBlurEnabled = false,
                    progressiveTopBlurEnabled = false,
                    progressiveTopFadeEnabled = false,
                ),
            ) {
                MaterialTheme {
                    MiuixTheme {
                        Box(Modifier.requiredSize(320.dp, 680.dp).testTag("following-viewport")) {
                            FollowingListScreen(
                                mid = 0L,
                                onBack = {},
                                onUserClick = {},
                                onFansClick = {},
                                viewModel = viewModel,
                            )
                        }
                    }
                }
            }
        }

        rule.onNodeWithText("管理").performClick()
        val title = bounds("我的关注")
        val search = bounds("搜索已加载 UP 主")
        assertTrue("Search must remain below the title", title.bottom <= search.top)
        assertActionsBelow(search, listOf("全选当前", "设置分组", "取消关注 (0)"))

        rule.onNodeWithText("全选当前").performClick()
        rule.onNodeWithText("已选 12 人").assertIsDisplayed()
        rule.onNodeWithText("取消关注 (12)").performClick()
        rule.onNodeWithText("确认取消关注已选择的 12 位 UP 主吗？").assertIsDisplayed()
        rule.onNodeWithText("取消", substring = false).performClick()

        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("关注测试 12"))
        val lastUser = bounds("关注测试 12")
        val labels = listOf("取消全选", "设置分组", "取消关注 (12)")
        assertActionsBelow(search, labels)
        val actionsTop = labels.minOf { bounds(it).top }
        assertTrue("The final user must scroll above the bottom actions", lastUser.bottom <= actionsTop)

        rule.onNodeWithText("完成").performClick()
        rule.onNodeWithText("全选当前").assertDoesNotExist()
        rule.onNodeWithText("设置分组").assertDoesNotExist()
        rule.onNodeWithText("取消关注 (12)").assertDoesNotExist()
    }

    private fun assertActionsBelow(search: Rect, labels: List<String>) {
        val viewport = rule.onNodeWithTag("following-viewport").fetchSemanticsNode().boundsInRoot
        val actionBounds = labels.map { label ->
            rule.onNodeWithText(label).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("Action labels must not be squeezed into vertical text", 1, layouts.single().lineCount)
            val layout = layouts.single()
            assertFalse("$label must not overflow vertically", layout.didOverflowHeight)
            // TextLayoutResult rounds its IntSize; allow only subpixel horizontal rounding.
            assertTrue(
                "$label clipped: lineRight=${layout.getLineRight(0)}, width=${layout.size.width}",
                layout.getLineRight(0) <= layout.size.width + 1f,
            )
            bounds(label).also { bounds ->
                assertTrue("Actions must not cover the search field", bounds.top >= search.bottom)
                assertTrue("Actions must stay inside the viewport", viewport.contains(bounds.topLeft) && bounds.right <= viewport.right && bounds.bottom <= viewport.bottom)
            }
        }
        actionBounds.forEachIndexed { index, bounds ->
            actionBounds.drop(index + 1).forEach { other ->
                assertFalse("Batch actions must not overlap each other", bounds.overlaps(other))
            }
        }
    }

    private fun bounds(text: String): Rect =
        rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
}
