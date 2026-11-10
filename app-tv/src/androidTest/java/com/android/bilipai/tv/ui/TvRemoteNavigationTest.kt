package com.android.bilipai.tv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.android.bilipai.tv.TvCatalogState
import com.android.bilipai.tv.tvId
import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Rule
import org.junit.Test

class TvRemoteNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val first = VideoItem(bvid = "BV-first", title = "第一个视频")
    private val second = VideoItem(bvid = "BV-second", title = "第二个视频")

    @Test fun confirmationAndReturnRestoreTheOriginalCard() {
        compose.setContent {
            var catalog by remember { mutableStateOf(TvCatalogState(items = listOf(first, second))) }
            var detail by remember { mutableStateOf(false) }
            val contentFocus = remember { FocusRequester() }
            val navFocus = remember { FocusRequester() }
            TvTheme {
                Box(Modifier.fillMaxSize()) {
                    if (detail) FocusButton("返回列表", { detail = false }, remember { FocusRequester() })
                    else TvVideoGrid(catalog, contentFocus, navFocus,
                        onOpen = { detail = true },
                        onFocused = { id -> catalog = catalog.copy(focusedId = id, focusedIndex = catalog.items.indexOfFirst { it.tvId() == id }) },
                        onScroll = { index, offset -> catalog = catalog.copy(firstVisibleIndex = index, firstVisibleOffset = offset) })
                }
            }
        }
        compose.onNodeWithTag("video:BV-first").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("video:BV-second").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("返回列表").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("video:BV-second").assertIsFocused()
    }

    @Test fun returningFromSidebarTargetsTheCurrentRow() {
        compose.setContent {
            var catalog by remember { mutableStateOf(TvCatalogState(items = listOf(first, second,
                VideoItem(bvid = "BV-third", title = "第三个视频"), VideoItem(bvid = "BV-fourth", title = "第四个视频")))) }
            val contentFocus = remember { FocusRequester() }
            val navFocus = remember { FocusRequester() }
            TvTheme {
                Row(Modifier.fillMaxSize()) {
                    Button(onClick = {}, modifier = Modifier.focusRequester(navFocus).focusProperties { right = contentFocus }) { Text("推荐") }
                    TvVideoGrid(catalog, contentFocus, navFocus, onOpen = {},
                        onFocused = { catalog = catalog.copy(focusedId = it) }, onScroll = { _, _ -> }, modifier = Modifier.width(500.dp))
                }
            }
        }
        compose.onNodeWithTag("video:BV-first").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("video:BV-third").assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithText("推荐").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("video:BV-third").assertIsFocused()
    }

    @Test fun appendingAPageDoesNotStealFocus() {
        val catalog = mutableStateOf(TvCatalogState(items = listOf(first, second)))
        compose.setContent {
            TvTheme {
                TvVideoGrid(catalog.value, remember { FocusRequester() }, remember { FocusRequester() },
                    onOpen = {}, onFocused = { catalog.value = catalog.value.copy(focusedId = it) }, onScroll = { _, _ -> })
            }
        }
        compose.onNodeWithTag("video:BV-first").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("video:BV-second").assertIsFocused()
        compose.runOnIdle {
            catalog.value = catalog.value.copy(items = catalog.value.items + VideoItem(bvid = "BV-third", title = "第三个视频"))
        }
        compose.onNodeWithTag("video:BV-second").assertIsFocused()
    }

    @Test fun scrollingNearTheEndLoadsTheNextPageAutomatically() {
        val catalog = mutableStateOf(TvCatalogState(items = listOf(first, second)))
        var loads = 0
        compose.setContent {
            TvTheme {
                TvVideoGrid(catalog.value, remember { FocusRequester() }, remember { FocusRequester() },
                    onOpen = {}, onFocused = {}, onScroll = { _, _ -> },
                    canLoadMore = true, onLoadMore = { loads++ })
            }
        }
        // 两项不足以填满一行：末行可见即触发一次自动分页
        compose.waitUntil(5_000) { loads > 0 }
        compose.runOnIdle {
            loads = 0
            catalog.value = catalog.value.copy(items = catalog.value.items + VideoItem(bvid = "BV-third", title = "第三个视频"))
        }
        compose.onNodeWithTag("video:BV-third").assertExists()
        compose.runOnIdle { check(loads == 0) { "内容更新不应重复触发分页" } }
    }
}
