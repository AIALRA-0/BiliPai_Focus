package com.android.purebilibili.feature.video.ui.overlay

internal enum class FullscreenShortcutKey {
    Space,
    Left,
    Right,
    Escape,
    KeyF,
    KeyM,
    KeyD,
    KeyL,
    Other,
}

internal enum class FullscreenKeyboardAction {
    PlayPause,
    SeekBackward,
    SeekForward,
    CloseTopLayer,
    ToggleFullscreen,
    ToggleMute,
    ToggleDanmaku,
    ToggleLock,
    None,
}

internal fun resolveFullscreenKeyboardAction(
    key: FullscreenShortcutKey,
    isKeyDown: Boolean,
    hasCommandModifier: Boolean,
    shortcutsEnabled: Boolean,
): FullscreenKeyboardAction {
    if (!isKeyDown || hasCommandModifier || !shortcutsEnabled) {
        return FullscreenKeyboardAction.None
    }
    return when (key) {
        FullscreenShortcutKey.Space -> FullscreenKeyboardAction.PlayPause
        FullscreenShortcutKey.Left -> FullscreenKeyboardAction.SeekBackward
        FullscreenShortcutKey.Right -> FullscreenKeyboardAction.SeekForward
        FullscreenShortcutKey.Escape -> FullscreenKeyboardAction.CloseTopLayer
        FullscreenShortcutKey.KeyF -> FullscreenKeyboardAction.ToggleFullscreen
        FullscreenShortcutKey.KeyM -> FullscreenKeyboardAction.ToggleMute
        FullscreenShortcutKey.KeyD -> FullscreenKeyboardAction.ToggleDanmaku
        FullscreenShortcutKey.KeyL -> FullscreenKeyboardAction.ToggleLock
        FullscreenShortcutKey.Other -> FullscreenKeyboardAction.None
    }
}
