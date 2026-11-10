package com.android.purebilibili.core.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.purebilibili.navigation.animatePagerSelection
import com.android.purebilibili.navigation.tabSelectedPage
import com.android.purebilibili.navigation.tabIndicatorPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TabSelectionMotionUiRegressionTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var pager: PagerState
    private lateinit var scope: CoroutineScope
    private val composedPages = mutableSetOf<Int>()

    private fun showPager() {
        rule.setContent {
            pager = rememberPagerState { 8 }
            scope = rememberCoroutineScope()
            HorizontalPager(state = pager, beyondViewportPageCount = 0, modifier = Modifier.size(300.dp, 400.dp)) { page ->
                SideEffect { composedPages += page }
                Box(Modifier.fillMaxSize())
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun distantClickKeepsRailContinuousWithoutMountingEveryIntermediatePage() {
        showPager()
        rule.runOnIdle { scope.launch { animatePagerSelection(pager, 7) } }
        var previous = 0f
        repeat(25) {
            rule.mainClock.advanceTimeByFrame()
            rule.runOnIdle {
                val position = pager.tabIndicatorPosition
                assertEquals(7, pager.tabSelectedPage)
                assertTrue("Rail must move continuously toward the clicked tab", position >= previous - 0.001f)
                assertTrue("Content pre-jumps must not jump the rail", position - previous < 1.5f)
                previous = position
            }
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(7, pager.settledPage)
            assertEquals(7f, pager.tabIndicatorPosition, 0.001f)
            assertFalse("Far clicks must skip intermediate content", composedPages.containsAll((1..6).toList()))
        }
    }

    @Test
    fun rapidRetargetAndCancellationReleaseTheRailToTheActualPager() {
        showPager()
        rule.runOnIdle { scope.launch { animatePagerSelection(pager, 7) } }
        rule.mainClock.advanceTimeBy(96)
        rule.runOnIdle { scope.launch { animatePagerSelection(pager, 1) } }
        rule.mainClock.advanceTimeBy(600)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(1, pager.settledPage)
            assertEquals(1f, pager.tabIndicatorPosition, 0.001f)
        }
        rule.mainClock.autoAdvance = false
        lateinit var pending: kotlinx.coroutines.Job
        rule.runOnIdle { pending = scope.launch { animatePagerSelection(pager, 6) } }
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { pending.cancel() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(pager.currentPage + pager.currentPageOffsetFraction, pager.tabIndicatorPosition, 0.001f)
        }
    }
}
