package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.purebilibili.core.ui.LocalVisualWorkAllowed
import org.junit.Rule
import org.junit.Test

class AudioNowPlayingPresenceWorkUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun retainedExitContentStopsVisualWorkAndInterruptedExitCanResume() {
        val visible = mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AudioNowPlayingBarPresenceHost(visible = visible.value) {
                BasicText(if (LocalVisualWorkAllowed.current) "active" else "paused")
            }
        }
        rule.onNodeWithText("active").assertExists()
        rule.runOnIdle { visible.value = false }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("paused").assertExists()
        rule.runOnIdle { visible.value = true }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("active").assertExists()
        rule.runOnIdle { visible.value = false }
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithText("paused").assertDoesNotExist()
    }
}
