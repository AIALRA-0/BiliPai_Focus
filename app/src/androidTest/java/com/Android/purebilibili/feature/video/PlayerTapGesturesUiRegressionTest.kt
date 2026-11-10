package com.android.purebilibili.feature.video

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.feature.video.ui.gesture.PlayerGestureInputSurface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.gesture.detectPlayerTapGestures
import com.android.purebilibili.danmaku.engine.DanmakuClickCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerTapGesturesUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var viewConfiguration: ViewConfiguration
    private var doubleTapSlopPx = 0f
    private val taps = mutableListOf<Offset>()
    private val doubleTaps = mutableListOf<Offset>()
    private var controlsVisible by mutableStateOf(false)
    private var paused = false
    private var playbackSpeed = 1f
    private var longPressStarts = 0

    private var overlayButtonClicks = 0
    private var nativeTouches = 0

    private fun setPlayerSurface(
        captureSingleTap: (Offset) -> DanmakuClickCapture? = { null },
        nativeRenderer: Boolean = false,
        initiallyVisibleControls: Boolean = false,
    ) {
        controlsVisible = initiallyVisibleControls
        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            doubleTapSlopPx = android.view.ViewConfiguration.get(LocalContext.current).scaledDoubleTapSlop.toFloat()
            Box(
                Modifier
                    .size(300.dp, 180.dp)
                    .testTag("player-tap-surface")
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                playbackSpeed = 2f
                                longPressStarts += 1
                            },
                            onDragEnd = { playbackSpeed = 1f },
                            onDragCancel = { playbackSpeed = 1f },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectPlayerTapGestures(
                            doubleTapSlopPx = doubleTapSlopPx,
                            captureSingleTap = captureSingleTap,
                            onTap = {
                                taps += it
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = {
                                doubleTaps += it
                                paused = !paused
                            }
                        )
                    }
            ) {
                if (nativeRenderer) {
                    // Simulate a rendering View taking touches when no Compose controls cover it.
                    AndroidView(
                        factory = { context ->
                            android.view.View(context).apply {
                                setOnTouchListener { _, _ -> nativeTouches += 1; true }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    PlayerGestureInputSurface(modifier = Modifier.fillMaxSize())
                    if (controlsVisible) {
                        Box(
                            Modifier.align(Alignment.BottomStart)
                                .size(48.dp)
                                .testTag("player-overlay-button")
                                .clickable { overlayButtonClicks += 1 },
                        )
                    }
                }
            }
        }
    }

    private fun finishTapWindow() {
        rule.mainClock.advanceTimeBy(viewConfiguration.doubleTapTimeoutMillis + 100L)
        rule.waitForIdle()
    }

    @Test
    fun nativeRendererWithHiddenControls_doubleTapPausesWithoutShowingControls() {
        setPlayerSurface(nativeRenderer = true)
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()
        rule.runOnIdle {
            assertTrue(paused)
            assertFalse(controlsVisible)
            assertEquals(1, doubleTaps.size)
            assertTrue(taps.isEmpty())
            assertEquals(0, nativeTouches)
        }
    }

    @Test
    fun nativeRenderer_singleTapThenDoubleTapKeepsControlsAndPlaybackIndependent() {
        setPlayerSurface(nativeRenderer = true)
        rule.onNodeWithTag("player-tap-surface").performTouchInput { click(center) }
        finishTapWindow()
        rule.runOnIdle { assertTrue(controlsVisible) }
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()
        rule.runOnIdle {
            assertTrue(paused)
            assertTrue(controlsVisible)
            assertEquals(1, taps.size)
            assertEquals(1, doubleTaps.size)
            assertEquals(0, nativeTouches)
        }
    }

    @Test
    fun visibleControls_doubleTapWithFingerRepositioning_pausesWithoutHidingControls() {
        setPlayerSurface(nativeRenderer = true)
        rule.onNodeWithTag("player-tap-surface").performTouchInput { click(center) }
        finishTapWindow()
        rule.runOnIdle { assertTrue(controlsVisible) }
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val delta = (viewConfiguration.touchSlop + doubleTapSlopPx) / 2f
            click(center - Offset(delta / 2f, 0f))
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center + Offset(delta / 2f, 0f))
        }
        finishTapWindow()
        rule.runOnIdle {
            assertTrue(paused)
            assertTrue(controlsVisible)
            assertEquals(1, taps.size)
            assertEquals(1, doubleTaps.size)
            assertEquals(0, nativeTouches)
        }
    }

    @Test
    fun overlayButtonAboveGestureSurface_keepsTouchPriority() {
        setPlayerSurface(nativeRenderer = true, initiallyVisibleControls = true)
        rule.onNodeWithTag("player-overlay-button").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()
        rule.runOnIdle {
            assertEquals(2, overlayButtonClicks)
            assertTrue(taps.isEmpty())
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }

    @Test
    fun capturedDanmakuTap_opensMenuWithoutTogglingControls() {
        var openedMenus = 0
        var releasedCaptures = 0
        setPlayerSurface(captureSingleTap = {
            DanmakuClickCapture(confirm = { openedMenus += 1 }, release = { releasedCaptures += 1 })
        })
        rule.onNodeWithTag("player-tap-surface").performTouchInput { click(center) }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, openedMenus)
            assertEquals(0, releasedCaptures)
            assertTrue(taps.isEmpty())
            assertTrue(doubleTaps.isEmpty())
            assertFalse(controlsVisible)
        }
    }

    @Test
    fun doubleTapOverDanmaku_discardsCapturedMenuAction() {
        var openedMenus = 0
        var releasedCaptures = 0
        setPlayerSurface(captureSingleTap = {
            DanmakuClickCapture(confirm = { openedMenus += 1 }, release = { releasedCaptures += 1 })
        })
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(0, openedMenus)
            assertEquals(1, releasedCaptures)
            assertEquals(1, doubleTaps.size)
            assertTrue(taps.isEmpty())
            assertTrue(paused)
            assertFalse(controlsVisible)
        }
    }

    @Test
    fun longPressRelease_thenImmediateTap_onlyShowsControls() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            longClick(center, durationMillis = viewConfiguration.longPressTimeoutMillis + 100L)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, longPressStarts)
            assertEquals(1f, playbackSpeed, 0f)
            assertTrue(controlsVisible)
            assertFalse(paused)
            assertEquals(1, taps.size)
            assertTrue(doubleTaps.isEmpty())
        }
    }

    @Test
    fun nearbyShortTaps_onlyTogglePlayback() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val second = center + Offset(viewConfiguration.touchSlop / 2f, 0f)
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(second)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertTrue(paused)
            assertFalse(controlsVisible)
            assertTrue(taps.isEmpty())
            assertEquals(1, doubleTaps.size)
            assertEquals(0, longPressStarts)
        }
    }

    @Test
    fun oppositeSides_areTwoIndependentTaps() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val left = Offset(width * 0.1f, center.y)
            val right = Offset(width * 0.9f, center.y)
            click(left)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(right)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(2, taps.size)
            assertTrue(taps.first().x < taps.last().x)
            assertFalse(controlsVisible)
            assertFalse(paused)
            assertTrue(doubleTaps.isEmpty())
        }
    }

    @Test
    fun distantSecondTap_canStartNearbyDoubleTap() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val left = Offset(width * 0.1f, center.y)
            val right = Offset(width * 0.9f, center.y)
            val third = right - Offset(viewConfiguration.touchSlop / 2f, 0f)
            click(left)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(right)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(third)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, taps.size)
            assertEquals(1, doubleTaps.size)
            assertTrue(taps.single().x < doubleTaps.single().x)
            assertTrue(controlsVisible)
            assertTrue(paused)
        }
    }

    @Test
    fun diagonalDistanceOutsideDoubleTapSlop_isNotDoubleTap() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val delta = doubleTapSlopPx * 0.8f
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center + Offset(delta, delta))
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(2, taps.size)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }

    @Test
    fun nearbySecondLongPress_thenTap_doesNotPausePlayback() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            longClick(center, durationMillis = viewConfiguration.longPressTimeoutMillis + 100L)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, longPressStarts)
            assertEquals(1f, playbackSpeed, 0f)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }

    @Test
    fun nearbyTapsOutsideTimeWindow_areIndependent() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis + 100L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(2, taps.size)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }
}
