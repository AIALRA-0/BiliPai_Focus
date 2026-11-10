package com.android.bilipai.tv.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Rule
import org.junit.Test

class TvHeroCarouselTest {
    @get:Rule val compose = createComposeRule()

    private val items = listOf(
        VideoItem(bvid = "BV-first", title = "第一个视频"),
        VideoItem(bvid = "BV-second", title = "第二个视频"),
    )

    @Test
    fun detailsFocusPausesAutomaticContentChanges() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TvTheme(reduceMotion = false) {
                TvHeroCarousel(items, FocusRequester(), onPlay = {}, onOpen = {}, onAmbientChange = {},
                    modifier = Modifier.fillMaxWidth().height(292.dp))
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("tv-hero-play")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("tv-hero-details").assertIsFocused()
        compose.mainClock.advanceTimeBy(8_000)
        compose.onNodeWithText("第一个视频").assertExists()
        compose.onNodeWithTag("tv-hero-details").assertIsFocused()
    }

    @Test
    fun manualAdvanceUsesRemoteConfirmationAndKeepsTheTriggerFocused() {
        compose.setContent {
            TvTheme(reduceMotion = true) {
                TvHeroCarousel(items, FocusRequester(), onPlay = {}, onOpen = {}, onAmbientChange = {},
                    modifier = Modifier.fillMaxWidth().height(292.dp))
            }
        }
        compose.onNodeWithTag("tv-hero-next")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("第二个视频").assertExists()
        compose.onNodeWithTag("tv-hero-next").assertIsFocused()
        compose.onNodeWithTag("tv-hero-next").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("第一个视频").assertExists()
        compose.onNodeWithTag("tv-hero-next").assertIsFocused()
    }
}
