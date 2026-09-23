package com.android.purebilibili.feature.video.player

import android.view.KeyEvent as AndroidKeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerKeyboardPolicyTest {

    private fun createKeyEvent(
        keyCode: Int,
        action: Int = AndroidKeyEvent.ACTION_DOWN,
        metaState: Int = 0,
    ): PlayerKeyInput {
        return PlayerKeyInput(
            keyCode = keyCode,
            isKeyDown = action == AndroidKeyEvent.ACTION_DOWN,
            isCtrlPressed = (metaState and AndroidKeyEvent.META_CTRL_ON) != 0,
            isAltPressed = (metaState and AndroidKeyEvent.META_ALT_ON) != 0,
            isMetaPressed = (metaState and AndroidKeyEvent.META_META_ON) != 0,
            isShiftPressed = (metaState and AndroidKeyEvent.META_SHIFT_ON) != 0,
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsPlaybackKeysCorrectly() {
        // Spacebar -> PlayPause
        assertEquals(
            PlayerKeyAction.PlayPause,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_SPACE))
        )
        // K -> PlayPause
        assertEquals(
            PlayerKeyAction.PlayPause,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_K))
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsSeekingKeysCorrectly() {
        // Right arrow -> SeekRelative(+5000ms)
        assertEquals(
            PlayerKeyAction.SeekRelative(KEYBOARD_SEEK_SHORT_STEP_MS),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        )
        // Left arrow -> SeekRelative(-5000ms)
        assertEquals(
            PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_SHORT_STEP_MS),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_DPAD_LEFT))
        )
        // Shift + Right arrow -> SeekRelative(+10000ms)
        assertEquals(
            PlayerKeyAction.SeekRelative(KEYBOARD_SEEK_LONG_STEP_MS),
            resolvePlayerKeyAction(
                createKeyEvent(
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                    metaState = AndroidKeyEvent.META_SHIFT_ON
                )
            )
        )
        // Shift + Left arrow -> SeekRelative(-10000ms)
        assertEquals(
            PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_LONG_STEP_MS),
            resolvePlayerKeyAction(
                createKeyEvent(
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                    metaState = AndroidKeyEvent.META_SHIFT_ON
                )
            )
        )
        // J -> SeekRelative(-10000ms)
        assertEquals(
            PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_LONG_STEP_MS),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_J))
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsInteractionKeysCorrectly() {
        // Q -> ToggleLike
        assertEquals(
            PlayerKeyAction.ToggleLike,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_Q))
        )
        // W -> Coin
        assertEquals(
            PlayerKeyAction.Coin,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_W))
        )
        // E -> ToggleFavorite
        assertEquals(
            PlayerKeyAction.ToggleFavorite,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_E))
        )
        // R -> TripleAction
        assertEquals(
            PlayerKeyAction.TripleAction,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_R))
        )
        // S -> TakeScreenshot
        assertEquals(
            PlayerKeyAction.TakeScreenshot,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_S))
        )
        // L -> ToggleScreenLock
        assertEquals(
            PlayerKeyAction.ToggleScreenLock,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_L))
        )
        // [ -> PreviousPart
        assertEquals(
            PlayerKeyAction.PreviousPart,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_LEFT_BRACKET))
        )
        // ] -> NextPart
        assertEquals(
            PlayerKeyAction.NextPart,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_RIGHT_BRACKET))
        )
        // Shift + 1 -> SetSpeed(1.0f)
        assertEquals(
            PlayerKeyAction.SetSpeed(1.0f),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_1, metaState = AndroidKeyEvent.META_SHIFT_ON))
        )
        // Shift + 2 -> SetSpeed(2.0f)
        assertEquals(
            PlayerKeyAction.SetSpeed(2.0f),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_2, metaState = AndroidKeyEvent.META_SHIFT_ON))
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsVolumeAndAudioKeysCorrectly() {
        // Up arrow -> VolumeUp
        assertEquals(
            PlayerKeyAction.VolumeUp,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_DPAD_UP))
        )
        // Down arrow -> VolumeDown
        assertEquals(
            PlayerKeyAction.VolumeDown,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_DPAD_DOWN))
        )
        // M -> ToggleMute
        assertEquals(
            PlayerKeyAction.ToggleMute,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_M))
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsFullscreenAndDanmakuKeysCorrectly() {
        // F -> ToggleFullscreen
        assertEquals(
            PlayerKeyAction.ToggleFullscreen,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_F))
        )
        // Enter -> ToggleFullscreen
        assertEquals(
            PlayerKeyAction.ToggleFullscreen,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_ENTER))
        )
        // D -> ToggleDanmaku
        assertEquals(
            PlayerKeyAction.ToggleDanmaku,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_D))
        )
    }

    @Test
    fun resolvePlayerKeyAction_mapsNumberPercentSeekingCorrectly() {
        // 0 -> SeekPercent(0.0f)
        assertEquals(
            PlayerKeyAction.SeekPercent(0.0f),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_0))
        )
        // 5 -> SeekPercent(0.5f)
        assertEquals(
            PlayerKeyAction.SeekPercent(0.5f),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_5))
        )
        // 9 -> SeekPercent(0.9f)
        assertEquals(
            PlayerKeyAction.SeekPercent(0.9f),
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_9))
        )
    }

    @Test
    fun resolvePlayerKeyAction_ignoresWhenDisabledOrSuppressed() {
        val spaceEvent = createKeyEvent(AndroidKeyEvent.KEYCODE_SPACE)

        // KeyUp event ignored
        val upEvent = createKeyEvent(
            AndroidKeyEvent.KEYCODE_SPACE,
            action = AndroidKeyEvent.ACTION_UP
        )
        assertNull(resolvePlayerKeyAction(upEvent))

        // Screen locked suppresses playback keys, but L unlocks
        assertNull(resolvePlayerKeyAction(spaceEvent, isScreenLocked = true))
        assertEquals(
            PlayerKeyAction.ToggleScreenLock,
            resolvePlayerKeyAction(createKeyEvent(AndroidKeyEvent.KEYCODE_L), isScreenLocked = true)
        )

        // In PiP mode
        assertNull(resolvePlayerKeyAction(spaceEvent, isInPipMode = true))

        // Text input active (e.g. typing danmaku or comment)
        assertNull(resolvePlayerKeyAction(spaceEvent, isTextInputActive = true))

        // System modifier pressed (Ctrl+Space, Alt+Space, Meta+Space)
        assertNull(
            resolvePlayerKeyAction(
                createKeyEvent(
                    AndroidKeyEvent.KEYCODE_SPACE,
                    metaState = AndroidKeyEvent.META_CTRL_ON
                )
            )
        )
        assertNull(
            resolvePlayerKeyAction(
                createKeyEvent(
                    AndroidKeyEvent.KEYCODE_SPACE,
                    metaState = AndroidKeyEvent.META_ALT_ON
                )
            )
        )
        assertNull(
            resolvePlayerKeyAction(
                createKeyEvent(
                    AndroidKeyEvent.KEYCODE_SPACE,
                    metaState = AndroidKeyEvent.META_META_ON
                )
            )
        )
    }

    @Test
    fun calculateSeekTargetPositionMs_computesCorrectClampedBounds() {
        val durationMs = 100_000L

        // Relative forward
        val forwardTarget = calculateSeekTargetPositionMs(
            currentPositionMs = 20_000L,
            durationMs = durationMs,
            action = PlayerKeyAction.SeekRelative(5_000L)
        )
        assertEquals(25_000L, forwardTarget)

        // Relative backward clamp to 0
        val underflowTarget = calculateSeekTargetPositionMs(
            currentPositionMs = 3_000L,
            durationMs = durationMs,
            action = PlayerKeyAction.SeekRelative(-5_000L)
        )
        assertEquals(0L, underflowTarget)

        // Relative forward clamp to duration
        val overflowTarget = calculateSeekTargetPositionMs(
            currentPositionMs = 98_000L,
            durationMs = durationMs,
            action = PlayerKeyAction.SeekRelative(5_000L)
        )
        assertEquals(durationMs, overflowTarget)

        // Percent seek
        val percentTarget = calculateSeekTargetPositionMs(
            currentPositionMs = 10_000L,
            durationMs = durationMs,
            action = PlayerKeyAction.SeekPercent(0.5f)
        )
        assertEquals(50_000L, percentTarget)

        // Zero duration returns null
        assertNull(
            calculateSeekTargetPositionMs(
                currentPositionMs = 0L,
                durationMs = 0L,
                action = PlayerKeyAction.SeekRelative(5_000L)
            )
        )
    }
}
