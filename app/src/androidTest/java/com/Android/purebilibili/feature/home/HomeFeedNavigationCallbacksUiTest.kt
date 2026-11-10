package com.android.purebilibili.feature.home

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class HomeFeedNavigationCallbacksUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun hostChangesKeepCardCallbacksStableButUseTheLatestNavigation() {
        val generation = mutableIntStateOf(1)
        val calls = mutableListOf<String>()
        lateinit var current: HomeFeedNavigationCallbacks
        var received: HomeVideoClickRequest? = null
        rule.setContent {
            val revision = generation.intValue
            val callbacks = rememberHomeFeedNavigationCallbacks(
                onVideoClick = { received = it; calls.add("video:$revision") },
                onUpClick = { calls.add("up:$revision:$it") },
                onLiveClick = { room, title, up -> calls.add("live:$revision:$room:$title:$up") },
            )
            SideEffect { current = callbacks }
        }
        rule.waitForIdle()
        val original = current
        rule.runOnIdle { generation.intValue = 2 }
        rule.waitForIdle()
        rule.runOnIdle {
            assertSame(original, current)
            assertSame(original.onVideoClick, current.onVideoClick)
            assertSame(original.onUpClick, current.onUpClick)
            assertSame(original.onLiveClick, current.onLiveClick)
            val request = HomeVideoClickRequest(
                bvid = "BV-current", cid = 456L, coverUrl = "current-cover",
                sourceRoute = "home?category=RECOMMEND",
            )
            original.onVideoClick(request)
            original.onUpClick(123L)
            original.onLiveClick(789L, "标题", "UP")
            assertSame(request, received)
            assertEquals(listOf("video:2", "up:2:123", "live:2:789:标题:UP"), calls)
        }
    }
}
