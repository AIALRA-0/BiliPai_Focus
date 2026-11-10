package com.android.purebilibili.feature.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeFeedStateCollectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pausedFeedKeepsDisplayedDataAndResumesFromLatestValue() {
        val source = MutableStateFlow("cached")
        var updatesAllowed by mutableStateOf(true)
        composeRule.setContent {
            val displayed by source.collectAsHomeFeedState(updatesAllowed)
            BasicText(displayed)
        }
        composeRule.onNodeWithText("cached").assertTextEquals("cached")
        composeRule.runOnIdle { updatesAllowed = false }
        composeRule.runOnIdle {
            assertEquals(0, source.subscriptionCount.value)
            source.value = "intermediate"
            source.value = "latest"
        }
        composeRule.onNodeWithText("cached").assertTextEquals("cached")
        composeRule.runOnIdle { updatesAllowed = true }
        composeRule.onNodeWithText("latest").assertTextEquals("latest")
    }

    @Test
    fun switchingSourceWhilePausedUsesItsOwnCacheAndRealZero() {
        val first = MutableStateFlow("previous category")
        val second = MutableStateFlow("0")
        var source by mutableStateOf<StateFlow<String>>(first)
        var updatesAllowed by mutableStateOf(false)
        composeRule.setContent {
            val displayed by source.collectAsHomeFeedState(updatesAllowed)
            BasicText(displayed)
        }
        composeRule.onNodeWithText("previous category").assertTextEquals("previous category")
        composeRule.runOnIdle { source = second }
        composeRule.onNodeWithText("0").assertTextEquals("0")
        composeRule.runOnIdle {
            assertEquals(0, first.subscriptionCount.value)
            assertEquals(0, second.subscriptionCount.value)
            first.value = "old request completed"
            second.value = "new category loaded"
        }
        composeRule.onNodeWithText("0").assertTextEquals("0")
        composeRule.runOnIdle { updatesAllowed = true }
        composeRule.onNodeWithText("new category loaded").assertTextEquals("new category loaded")
    }
}
