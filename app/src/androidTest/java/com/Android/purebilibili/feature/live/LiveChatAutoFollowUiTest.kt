package com.android.purebilibili.feature.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.live.components.LiveChatAutoFollow
import kotlinx.coroutines.flow.collect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LiveChatAutoFollowUiTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var listState: LazyListState
    private var messages by mutableStateOf((1L..60L).toList())
    private var following by mutableStateOf(true)
    private var paused by mutableStateOf(false)
    private var active by mutableStateOf(true)
    private var injectDuringScroll by mutableStateOf(false)

    @Test
    fun menuCloseAndPageResumeCatchUpWithoutAnotherMessage() {
        showChat()
        awaitBottom()
        rule.runOnIdle {
            paused = true
            messages = (1L..90L).toList()
        }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(listState.canScrollForward) }
        rule.runOnIdle { paused = false }
        awaitBottom()

        rule.runOnIdle {
            active = false
            messages = (1L..120L).toList()
        }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(listState.canScrollForward) }
        rule.runOnIdle { active = true }
        awaitBottom()
    }

    @Test
    fun cappedHistoryCanReceiveMoreMessagesDuringTheScroll() {
        showChat()
        awaitBottom()
        rule.runOnIdle {
            injectDuringScroll = true
            messages = (21L..80L).toList()
        }
        rule.waitUntil(5_000) { !injectDuringScroll }
        awaitBottom()
        rule.runOnIdle {
            assertEquals(100L, listState.layoutInfo.visibleItemsInfo.last().key)
            assertTrue(following)
        }
    }

    @Test
    fun manualScrollPausesFollowingAndExplicitResumeCatchesUp() {
        showChat()
        awaitBottom()
        rule.onNodeWithTag("chat").performTouchInput { swipeDown() }
        rule.waitUntil(5_000) { !following && !listState.isScrollInProgress }
        var firstKey: Any? = null
        rule.runOnIdle {
            firstKey = listState.layoutInfo.visibleItemsInfo.first().key
            messages = (1L..90L).toList()
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(firstKey, listState.layoutInfo.visibleItemsInfo.first().key)
            assertFalse(following)
            following = true
        }
        awaitBottom()
    }

    private fun showChat() {
        rule.setContent {
            listState = rememberLazyListState()
            LiveChatAutoFollow(
                listState = listState,
                lastSequence = messages.lastOrNull(),
                messageCount = messages.size,
                followingLatest = following,
                paused = paused,
                active = active,
                onFollowingLatestChange = { following = it },
            )
            // 在实际自动滚动开始后更新固定长度历史，验证不能仅依赖列表长度。
            LaunchedEffect(listState) {
                snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
                    if (scrolling && injectDuringScroll) {
                        injectDuringScroll = false
                        messages = (41L..100L).toList()
                    }
                }
            }
            LazyColumn(Modifier.size(220.dp, 160.dp).testTag("chat"), state = listState) {
                items(messages, key = { it }) {
                    Box(Modifier.fillMaxWidth().height(32.dp))
                }
            }
        }
    }

    private fun awaitBottom() {
        rule.waitUntil(5_000) {
            listState.layoutInfo.totalItemsCount == messages.size &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.key == messages.lastOrNull() &&
                !listState.isScrollInProgress && !listState.canScrollForward
        }
    }
}
