package com.android.purebilibili.feature.video

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** First input must be mouse: an earlier touch drag initializes the channel and masks the crash. */
@RunWith(AndroidJUnit4::class)
class ComposeDragStartGuardUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun firstMouseDragOnFreshListIsIgnoredAndWheelStillScrolls() {
        lateinit var state: LazyListState
        rule.setContent {
            state = rememberLazyListState()
            LazyColumn(
                state = state,
                modifier = Modifier.size(240.dp).testTag("fresh-list"),
            ) {
                items(40) { Box(Modifier.fillMaxWidth().height(48.dp)) }
            }
        }
        val list = rule.onNodeWithTag("fresh-list")
        list.performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, -80f), delayMillis = 64L)
            release()
        }
        rule.runOnIdle {
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
        list.performMouseInput {
            moveTo(center)
            scroll(4f)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue(state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0)
        }
    }

    @Test
    fun firstMouseDragOnFreshHorizontalStripIsIgnoredAndTouchStillScrolls() {
        lateinit var state: ScrollState
        rule.setContent {
            state = rememberScrollState()
            Row(
                modifier = Modifier
                    .size(240.dp, 80.dp)
                    .testTag("fresh-strip")
                    .horizontalScroll(state),
            ) {
                repeat(40) { Box(Modifier.size(80.dp)) }
            }
        }
        val strip = rule.onNodeWithTag("fresh-strip")
        strip.performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-80f, 0f), delayMillis = 64L)
            release()
        }
        rule.runOnIdle { assertEquals(0, state.value) }
        strip.performTouchInput { swipeLeft() }
        rule.runOnIdle { assertTrue(state.value > 0) }
    }
}
