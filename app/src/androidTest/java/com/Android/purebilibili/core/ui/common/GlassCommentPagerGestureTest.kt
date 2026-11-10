package com.android.purebilibili.core.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.components.AppHorizontalPager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlassCommentPagerGestureTest {
    @get:Rule
    val rule = createComposeRule()
    private lateinit var pager: PagerState
    private lateinit var comments: LazyListState

    @Test
    fun miuixDiagonalSwipeScrollsComments() = checkDiagonalSwipe(AppUiStyle.MIUIX)

    @Test
    fun materialDiagonalSwipeScrollsComments() = checkDiagonalSwipe(AppUiStyle.MATERIAL3)

    @Test
    fun miuixHorizontalSwipeStillChangesPage() = checkHorizontalSwipe(AppUiStyle.MIUIX)

    @Test
    fun materialHorizontalSwipeStillChangesPage() = checkHorizontalSwipe(AppUiStyle.MATERIAL3)

    private fun setContent(style: AppUiStyle) {
        rule.setContent {
            CompositionLocalProvider(LocalAppUiStyle provides style) {
                pager = rememberPagerState(initialPage = 1) { 2 }
                comments = rememberLazyListState()
                AppHorizontalPager(
                    state = pager,
                    // Same ownership contract as the glass detail screen: only the
                    // direction-gated detector can drive touch paging.
                    userScrollEnabled = false,
                    modifier = Modifier.size(320.dp, 480.dp)
                        .verticalPriorityHorizontalPagerSwipe(
                            state = pager,
                            enabled = true,
                            forceVerticalPriority = true,
                        ),
                ) { page ->
                    if (page == 1) {
                        LazyColumn(state = comments, modifier = Modifier.fillMaxSize().testTag("comments")) {
                            items(100) { Box(Modifier.fillMaxWidth().height(64.dp)) }
                        }
                    } else Box(Modifier.fillMaxSize())
                }
            }
        }
    }

    private fun checkDiagonalSwipe(style: AppUiStyle) {
        setContent(style)
        rule.onNodeWithTag("comments").performTouchInput {
            swipe(Offset(width * 0.7f, height * 0.8f), Offset(width * 0.4f, height * 0.2f), 400)
        }
        rule.runOnIdle {
            assertEquals(1, pager.currentPage)
            assertEquals(0f, pager.currentPageOffsetFraction, 0.001f)
            assertTrue(comments.firstVisibleItemIndex > 0 || comments.firstVisibleItemScrollOffset > 0)
        }
    }

    private fun checkHorizontalSwipe(style: AppUiStyle) {
        setContent(style)
        rule.onNodeWithTag("comments").performTouchInput {
            swipe(Offset(width * 0.15f, height * 0.5f), Offset(width * 0.85f, height * 0.52f), 400)
        }
        rule.runOnIdle {
            assertEquals(0, pager.currentPage)
            assertEquals(0, comments.firstVisibleItemIndex)
            assertEquals(0, comments.firstVisibleItemScrollOffset)
        }
    }
}
