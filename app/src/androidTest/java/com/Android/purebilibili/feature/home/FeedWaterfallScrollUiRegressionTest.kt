package com.android.purebilibili.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.components.FeedVerticalStaggeredGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedWaterfallScrollUiRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoUnevenLanes_keepTheirRelativePositionsDuringSlowReverseScroll() {
        assertSlowScrollStability(columns = 2)
    }

    @Test
    fun threeUnevenLanes_keepTheirRelativePositionsDuringSlowReverseScroll() {
        assertSlowScrollStability(columns = 3)
    }

    @Test
    fun singleLane_preservesTopInsetAndScrollToTop() {
        assertSlowScrollStability(columns = 1)
    }

    @Test
    fun singleLane_incrementalRefreshKeepsReadingPosition() {
        assertIncrementalRefreshStability(columns = 1)
    }

    @Test
    fun twoUnevenLanes_incrementalRefreshKeepsReadingPosition() {
        assertIncrementalRefreshStability(columns = 2)
    }

    @Test
    fun threeUnevenLanes_incrementalRefreshKeepsReadingPosition() {
        assertIncrementalRefreshStability(columns = 3)
    }

    private fun assertIncrementalRefreshStability(columns: Int) {
        lateinit var state: LazyStaggeredGridState
        var cards by mutableStateOf(List(40) { "old_$it" })
        var dividerIndex by mutableStateOf(-1)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLazyStaggeredGridState()
                FeedVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = state,
                    modifier = Modifier.requiredSize(720.dp, 480.dp),
                    contentPadding = PaddingValues(top = 84.dp, bottom = 80.dp),
                    verticalItemSpacing = 12.dp,
                    prependItemKeys = cards,
                    prependDividerIndex = dividerIndex,
                ) {
                    cards.forEachIndexed { index, key ->
                        if (index == dividerIndex) {
                            item(key = "old_content_divider", span = StaggeredGridItemSpan.FullLine) {
                                Box(Modifier.fillMaxWidth().height(36.dp))
                            }
                        }
                        item(key = key) {
                            val height = listOf(110, 330, 160, 490, 220)[key.substringAfterLast('_').toInt() % 5]
                            Box(Modifier.fillMaxWidth().height(height.dp))
                        }
                    }
                }
            }
        }

        fun cardY(key: Any): Int = state.layoutInfo.visibleItemsInfo.first { it.key == key }.offset.y
        var readingKey: Any = "old_0"
        var readingY = 84

        // A one-item prepend makes the destination overlap the OLD visible index range.
        // Repeat with larger batches while the old card/divider remains in the viewport.
        for (batchSize in listOf(1, 7, 2)) {
            composeRule.runOnIdle {
                val readingCard = state.layoutInfo.visibleItemsInfo.first {
                    it.key in cards && it.offset.y + it.size.height > 84
                }
                readingKey = readingCard.key
                readingY = readingCard.offset.y
                cards = List(batchSize) { "batch${cards.size}_$it" } + cards
                dividerIndex = batchSize
            }
            composeRule.runOnIdle {
                assertEquals("Refresh must preserve the reading card's pixel position", readingY, cardY(readingKey))
                val newIndex = cards.indexOf(readingKey) + 2 // Chrome + divider.
                assertEquals(newIndex, state.layoutInfo.visibleItemsInfo.first { it.key == readingKey }.index)
                assertTrue("New content must be reachable above the old card", state.canScrollBackward)
            }
        }

        composeRule.runOnIdle {
            cards = cards + List(10) { "next_$it" }
        }
        composeRule.runOnIdle {
            assertEquals("Loading another page must not move the viewport", readingY, cardY(readingKey))
        }
        composeRule.runOnIdle { state.dispatchRawDelta(35f) }
        composeRule.runOnIdle {
            assertEquals("Scrolling must remain usable after restoration", readingY - 35, cardY(readingKey))
        }
    }

    private fun assertSlowScrollStability(columns: Int) {
        lateinit var state: LazyStaggeredGridState
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLazyStaggeredGridState()
                FeedVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = state,
                    modifier = Modifier.requiredSize(720.dp, 480.dp),
                    contentPadding = PaddingValues(top = 84.dp, bottom = 80.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    items(count = 80, key = { "card_$it" }) { index ->
                        // Fixed geometry excludes network loading, image decoding and text changes.
                        val height = listOf(110, 330, 160, 490, 220)[index % 5]
                        Box(Modifier.fillMaxWidth().height(height.dp))
                    }
                }
            }
        }

        fun positions(): Map<Any, ItemPosition> = state.layoutInfo.visibleItemsInfo
            .filter { it.key.toString().startsWith("card_") }
            .associate { it.key to ItemPosition(it.lane, it.offset.y, it.size.height) }

        composeRule.runOnIdle {
            val initial = positions()
            repeat(columns) { index ->
                assertEquals(
                    "First row must keep the original chrome inset",
                    84,
                    initial.getValue("card_$index").y,
                )
            }
        }

        // Cross many item boundaries with tiny deltas, repeatedly reversing direction. A lane
        // correction can look like image flicker, so check whole-item geometry after EVERY step.
        repeat(2) {
            for (direction in listOf(1f, -1f)) {
                repeat(240) {
                    var before = emptyMap<Any, ItemPosition>()
                    var consumed = 0f
                    composeRule.runOnIdle {
                        before = positions()
                        consumed = state.dispatchRawDelta(3f * direction)
                    }
                    composeRule.runOnIdle {
                        val after = positions()
                        val commonKeys = before.keys.intersect(after.keys)
                        assertTrue("Must compare surviving visible cards", commonKeys.isNotEmpty())
                        commonKeys.forEach { key ->
                            val old = before.getValue(key)
                            val new = after.getValue(key)
                            assertEquals("$key changed lane", old.lane, new.lane)
                            assertEquals("$key changed height", old.height, new.height)
                            assertEquals(
                                "$key jumped independently of the scroll",
                                -consumed,
                                (new.y - old.y).toFloat(),
                                1f,
                            )
                        }
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(84, positions().getValue("card_0").y)
            assertTrue("Reverse scroll must return to the start", !state.canScrollBackward)
        }
    }

    private data class ItemPosition(val lane: Int, val y: Int, val height: Int)
}
