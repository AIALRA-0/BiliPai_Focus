package com.android.purebilibili.feature.video.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

sealed interface PlayerKeyAction {
    data object PlayPause : PlayerKeyAction
    data class SeekRelative(val deltaMs: Long) : PlayerKeyAction
    data class SeekPercent(val fraction: Float) : PlayerKeyAction
    data object VolumeUp : PlayerKeyAction
    data object VolumeDown : PlayerKeyAction
    data object ToggleMute : PlayerKeyAction
    data object ToggleFullscreen : PlayerKeyAction
    data object ToggleDanmaku : PlayerKeyAction
    data object ToggleLike : PlayerKeyAction
    data object Coin : PlayerKeyAction
    data object ToggleFavorite : PlayerKeyAction
    data object TripleAction : PlayerKeyAction
    data object TakeScreenshot : PlayerKeyAction
    data object ToggleScreenLock : PlayerKeyAction
    data object PreviousPart : PlayerKeyAction
    data object NextPart : PlayerKeyAction
    data class SetSpeed(val speed: Float) : PlayerKeyAction
}

internal data class PlayerKeyInput(
    val keyCode: Int,
    val isKeyDown: Boolean,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val isMetaPressed: Boolean = false,
    val isShiftPressed: Boolean = false,
)

internal const val KEYBOARD_SEEK_SHORT_STEP_MS = 5_000L
internal const val KEYBOARD_SEEK_LONG_STEP_MS = 10_000L

internal fun resolvePlayerKeyAction(
    event: KeyEvent,
    isScreenLocked: Boolean = false,
    isInPipMode: Boolean = false,
    isTextInputActive: Boolean = false,
): PlayerKeyAction? = resolvePlayerKeyAction(
    input = PlayerKeyInput(
        keyCode = event.nativeKeyEvent.keyCode,
        isKeyDown = event.type == KeyEventType.KeyDown,
        isCtrlPressed = event.isCtrlPressed,
        isAltPressed = event.isAltPressed,
        isMetaPressed = event.isMetaPressed,
        isShiftPressed = event.isShiftPressed,
    ),
    isScreenLocked = isScreenLocked,
    isInPipMode = isInPipMode,
    isTextInputActive = isTextInputActive,
)

internal fun resolvePlayerKeyAction(
    input: PlayerKeyInput,
    isScreenLocked: Boolean = false,
    isInPipMode: Boolean = false,
    isTextInputActive: Boolean = false,
): PlayerKeyAction? {
    if (!input.isKeyDown) return null
    if (isInPipMode || isTextInputActive) return null

    // Screen locked: only L (unlock) is accepted
    if (isScreenLocked) {
        return if (input.keyCode == AndroidKeyEvent.KEYCODE_L &&
            !input.isCtrlPressed && !input.isAltPressed && !input.isMetaPressed
        ) {
            PlayerKeyAction.ToggleScreenLock
        } else {
            null
        }
    }

    // Do not intercept system-level shortcuts (Ctrl, Alt, Meta/Cmd)
    if (input.isCtrlPressed || input.isAltPressed || input.isMetaPressed) {
        return null
    }

    val shift = input.isShiftPressed

    if (shift) {
        return when (input.keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_LONG_STEP_MS)
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> PlayerKeyAction.SeekRelative(KEYBOARD_SEEK_LONG_STEP_MS)
            AndroidKeyEvent.KEYCODE_1, AndroidKeyEvent.KEYCODE_NUMPAD_1 -> PlayerKeyAction.SetSpeed(1.0f)
            AndroidKeyEvent.KEYCODE_2, AndroidKeyEvent.KEYCODE_NUMPAD_2 -> PlayerKeyAction.SetSpeed(2.0f)
            else -> null
        }
    }

    return when (input.keyCode) {
        AndroidKeyEvent.KEYCODE_SPACE, AndroidKeyEvent.KEYCODE_K -> PlayerKeyAction.PlayPause
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_SHORT_STEP_MS)
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> PlayerKeyAction.SeekRelative(KEYBOARD_SEEK_SHORT_STEP_MS)
        AndroidKeyEvent.KEYCODE_J -> PlayerKeyAction.SeekRelative(-KEYBOARD_SEEK_LONG_STEP_MS)
        AndroidKeyEvent.KEYCODE_DPAD_UP -> PlayerKeyAction.VolumeUp
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> PlayerKeyAction.VolumeDown
        AndroidKeyEvent.KEYCODE_F, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ->
            PlayerKeyAction.ToggleFullscreen
        AndroidKeyEvent.KEYCODE_M -> PlayerKeyAction.ToggleMute
        AndroidKeyEvent.KEYCODE_D -> PlayerKeyAction.ToggleDanmaku
        AndroidKeyEvent.KEYCODE_Q -> PlayerKeyAction.ToggleLike
        AndroidKeyEvent.KEYCODE_W -> PlayerKeyAction.Coin
        AndroidKeyEvent.KEYCODE_E -> PlayerKeyAction.ToggleFavorite
        AndroidKeyEvent.KEYCODE_R -> PlayerKeyAction.TripleAction
        AndroidKeyEvent.KEYCODE_S -> PlayerKeyAction.TakeScreenshot
        AndroidKeyEvent.KEYCODE_L -> PlayerKeyAction.ToggleScreenLock
        AndroidKeyEvent.KEYCODE_LEFT_BRACKET -> PlayerKeyAction.PreviousPart
        AndroidKeyEvent.KEYCODE_RIGHT_BRACKET -> PlayerKeyAction.NextPart
        AndroidKeyEvent.KEYCODE_0, AndroidKeyEvent.KEYCODE_NUMPAD_0 -> PlayerKeyAction.SeekPercent(0.0f)
        AndroidKeyEvent.KEYCODE_1, AndroidKeyEvent.KEYCODE_NUMPAD_1 -> PlayerKeyAction.SeekPercent(0.1f)
        AndroidKeyEvent.KEYCODE_2, AndroidKeyEvent.KEYCODE_NUMPAD_2 -> PlayerKeyAction.SeekPercent(0.2f)
        AndroidKeyEvent.KEYCODE_3, AndroidKeyEvent.KEYCODE_NUMPAD_3 -> PlayerKeyAction.SeekPercent(0.3f)
        AndroidKeyEvent.KEYCODE_4, AndroidKeyEvent.KEYCODE_NUMPAD_4 -> PlayerKeyAction.SeekPercent(0.4f)
        AndroidKeyEvent.KEYCODE_5, AndroidKeyEvent.KEYCODE_NUMPAD_5 -> PlayerKeyAction.SeekPercent(0.5f)
        AndroidKeyEvent.KEYCODE_6, AndroidKeyEvent.KEYCODE_NUMPAD_6 -> PlayerKeyAction.SeekPercent(0.6f)
        AndroidKeyEvent.KEYCODE_7, AndroidKeyEvent.KEYCODE_NUMPAD_7 -> PlayerKeyAction.SeekPercent(0.7f)
        AndroidKeyEvent.KEYCODE_8, AndroidKeyEvent.KEYCODE_NUMPAD_8 -> PlayerKeyAction.SeekPercent(0.8f)
        AndroidKeyEvent.KEYCODE_9, AndroidKeyEvent.KEYCODE_NUMPAD_9 -> PlayerKeyAction.SeekPercent(0.9f)
        else -> null
    }
}

internal fun calculateSeekTargetPositionMs(
    currentPositionMs: Long,
    durationMs: Long,
    action: PlayerKeyAction,
): Long? {
    if (durationMs <= 0L) return null
    val current = currentPositionMs.coerceAtLeast(0L)
    return when (action) {
        is PlayerKeyAction.SeekRelative -> {
            (current + action.deltaMs).coerceIn(0L, durationMs)
        }
        is PlayerKeyAction.SeekPercent -> {
            (durationMs * action.fraction).toLong().coerceIn(0L, durationMs)
        }
        else -> null
    }
}
