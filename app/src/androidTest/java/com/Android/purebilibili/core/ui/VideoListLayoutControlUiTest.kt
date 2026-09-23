package com.Android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.android.purebilibili.core.ui.components.videoListItemModifier
import com.android.purebilibili.core.ui.components.AnimatedVideoListItem
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.components.cards.HomeStyleSingleColumnVideoCard
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.ProvideAppThemeConfig
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.VideoListLayoutToggle
import com.android.purebilibili.core.ui.components.rememberVideoListLayoutControl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class VideoListLayoutControlUiTest {
    @get:Rule
    val rule = createComposeRule()

    private fun content(style: AppUiStyle, animated: Boolean) {
        rule.setContent {
            CompositionLocalProvider(LocalAppUiStyle provides style) {
                ProvideAppThemeConfig(AppThemeConfig(uiEntranceAnimationEnabled = animated)) {
                    MaterialTheme {
                        MiuixTheme {
                            val layout = rememberVideoListLayoutControl()
                            Column {
                                VideoListLayoutToggle(layout.singleColumn, layout.toggle)
                                AppText(if (layout.singleColumn) "单列内容" else "双列内容")
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(if (layout.singleColumn) 1 else 2),
                                    modifier = Modifier.width(320.dp).height(360.dp).testTag("video-list"),
                                ) {
                                    items(listOf("a", "b", "c", "d"), key = { it }) { id ->
                                        AnimatedVideoListItem(modifier = videoListItemModifier()) {
                                            Box(modifier = Modifier.fillMaxWidth().height(if (layout.singleColumn) 72.dp else 140.dp).testTag("card-$id")) {
                                                AppText(id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun horizontalCardOverflowIsAtBottomWithFullTouchTarget() {
        rule.setContent {
            MaterialTheme {
                MiuixTheme {
                    HomeStyleSingleColumnVideoCard(
                        video = VideoItem(bvid = "BV_layout", title = "用于验证底部菜单位置的视频标题"),
                        sourceRoute = "history",
                        coverAspectRatio = 1.6f,
                        transitionEnabled = false,
                        modifier = Modifier.width(360.dp).testTag("card"),
                        onClick = {},
                        onMoreClick = {},
                    )
                }
            }
        }
        val menu = rule.onNodeWithContentDescription("更多操作")
        menu.assertHeightIsAtLeast(48.dp)
        val cardBounds = rule.onNodeWithTag("card").fetchSemanticsNode().boundsInRoot
        val menuBounds = menu.fetchSemanticsNode().boundsInRoot
        assertTrue(menuBounds.center.y > cardBounds.center.y)
        assertTrue(menuBounds.center.x > cardBounds.center.x)
    }

    @Test
    fun materialControlSwitchesBothWaysWithAnimationsDisabled() {
        content(AppUiStyle.MATERIAL3, animated = false)
        rule.onNodeWithContentDescription("切换为单列").performClick()
        rule.onNodeWithText("单列内容").assertIsDisplayed()
        rule.onNodeWithContentDescription("切换为双列").performClick()
        rule.onNodeWithText("双列内容").assertIsDisplayed()
    }

    @Test
    fun miuixTransitionKeepsOneContentTreeAndCanReverseMidFlight() {
        content(AppUiStyle.MIUIX, animated = true)
        val initialWidth = rule.onNodeWithTag("card-a").fetchSemanticsNode().boundsInRoot.width
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("切换为单列").performClick()
        rule.mainClock.advanceTimeBy(48)
        rule.onAllNodesWithTag("video-list").assertCountEquals(1)
        listOf("a", "b", "c", "d").forEach { id ->
            rule.onAllNodesWithTag("card-$id").assertCountEquals(1)
        }
        val movingCard = rule.onNodeWithTag("card-b").fetchSemanticsNode().boundsInRoot
        val firstCard = rule.onNodeWithTag("card-a").fetchSemanticsNode().boundsInRoot
        assertTrue("Second card should still be moving toward the first column", movingCard.left > firstCard.left)
        assertTrue("Card width should interpolate instead of jumping", firstCard.width > initialWidth && firstCard.width < initialWidth * 2)
        rule.onNodeWithContentDescription("切换为双列").performClick()
        rule.mainClock.advanceTimeBy(1200)
        rule.onNodeWithText("双列内容").assertIsDisplayed()
        rule.onAllNodesWithTag("video-list").assertCountEquals(1)
        rule.onNodeWithContentDescription("切换为单列").performClick()
        rule.mainClock.advanceTimeBy(1200)
        rule.onNodeWithText("单列内容").assertIsDisplayed()
        rule.onAllNodesWithTag("video-list").assertCountEquals(1)
    }
}
