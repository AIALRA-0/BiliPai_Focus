package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.LocalVisualWorkAllowed
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackBarsIndicatorTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun retainedInactivePageStopsDrawingAndResumesWhenSelected() {
        val active = mutableStateOf(true)
        val draws = AtomicInteger()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(
                LocalVisualWorkAllowed provides active.value,
                LocalComponentMotionEnabled provides true,
                LocalSystemReduceMotion provides false,
            ) {
                PlaybackBarsIndicator(
                    isPlaying = true,
                    color = Color.White,
                    contentDescription = "正在直播",
                    modifier = Modifier.drawWithContent {
                        draws.incrementAndGet()
                        drawContent()
                    },
                )
            }
        }
        settle()
        assertContinuesDrawing(draws)
        composeRule.runOnIdle { active.value = false }
        settle()
        assertStopsDrawing(draws)
        composeRule.runOnIdle { active.value = true }
        settle()
        assertContinuesDrawing(draws)
    }

    @Test
    fun offscreenAndPausedIndicatorsStayStatic() {
        val offscreen = mutableStateOf(false)
        val playing = mutableStateOf(true)
        val draws = AtomicInteger()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(
                LocalVisualWorkAllowed provides true,
                LocalComponentMotionEnabled provides true,
                LocalSystemReduceMotion provides false,
            ) {
                Box(Modifier.size(48.dp)) {
                    PlaybackBarsIndicator(
                        isPlaying = playing.value,
                        color = Color.White,
                        contentDescription = "正在直播",
                        modifier = Modifier
                            .offset(x = if (offscreen.value) 10_000.dp else 0.dp)
                            .drawWithContent {
                                draws.incrementAndGet()
                                drawContent()
                            },
                    )
                }
            }
        }
        settle()
        assertContinuesDrawing(draws)
        composeRule.runOnIdle { offscreen.value = true }
        settle()
        assertStopsDrawing(draws)
        composeRule.runOnIdle {
            offscreen.value = false
            playing.value = false
        }
        settle()
        assertStopsDrawing(draws)
    }

    private fun settle() {
        repeat(4) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }

    private fun assertStopsDrawing(draws: AtomicInteger) {
        val before = draws.get()
        composeRule.mainClock.advanceTimeBy(250)
        composeRule.waitForIdle()
        assertEquals(before, draws.get())
    }

    private fun assertContinuesDrawing(draws: AtomicInteger) {
        val before = draws.get()
        // Flush Android drawing between test frames instead of conflating them into one draw.
        repeat(4) {
            composeRule.mainClock.advanceTimeBy(64)
            composeRule.waitForIdle()
        }
        assertTrue(draws.get() > before)
    }
}
