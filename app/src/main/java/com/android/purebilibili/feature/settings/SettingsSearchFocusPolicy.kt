package com.android.purebilibili.feature.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsSearchFocusIds {
    const val APPEARANCE_THEME = "appearance_theme"
    const val APPEARANCE_DISPLAY = "appearance_display"
    const val APPEARANCE_SPLASH = "appearance_splash"
    const val APPEARANCE_PERSONALIZATION = "appearance_personalization"
    const val APPEARANCE_TABLET = "appearance_tablet"
    const val APPEARANCE_HOME = "appearance_home"

    const val PLAYBACK_DECODER = "playback_decoder"
    const val PLAYBACK_SPEED = "playback_speed"
    const val PLAYBACK_MINI_PLAYER = "playback_mini_player"
    const val PLAYBACK_GESTURE = "playback_gesture"
    const val PLAYBACK_DEBUG = "playback_debug"
    const val PLAYBACK_INTERACTION = "playback_interaction"
    const val PLAYBACK_FULLSCREEN = "playback_fullscreen"
    const val PLAYBACK_NETWORK = "playback_network"
    const val PLAYBACK_DATA_SAVER = "playback_data_saver"

    const val BOTTOM_BAR_DISPLAY = "bottom_bar_display"
    const val BOTTOM_BAR_TOP_TABS = "bottom_bar_top_tabs"
    const val BOTTOM_BAR_CURRENT = "bottom_bar_current"
    const val BOTTOM_BAR_AVAILABLE = "bottom_bar_available"
}

data class SettingsSearchFocusRequest(
    val target: SettingsSearchTarget,
    val focusId: String,
    val token: Long = System.nanoTime()
)

object SettingsSearchFocusController {
    private val _request = MutableStateFlow<SettingsSearchFocusRequest?>(null)
    val request = _request.asStateFlow()

    fun submit(target: SettingsSearchTarget, focusId: String?) {
        if (focusId.isNullOrBlank()) {
            _request.value = null
            return
        }
        _request.value = SettingsSearchFocusRequest(target = target, focusId = focusId)
    }

    fun clear(token: Long? = null) {
        val current = _request.value ?: return
        if (token == null || current.token == token) {
            _request.value = null
        }
    }
}

internal fun resolveAppearanceSettingsScrollIndex(
    focusId: String,
    isTablet: Boolean
): Int? {
    return when (focusId) {
        SettingsSearchFocusIds.APPEARANCE_THEME -> 0
        SettingsSearchFocusIds.APPEARANCE_DISPLAY -> 2
        SettingsSearchFocusIds.APPEARANCE_SPLASH -> 4
        SettingsSearchFocusIds.APPEARANCE_PERSONALIZATION -> 6
        SettingsSearchFocusIds.APPEARANCE_TABLET -> if (isTablet) 8 else null
        SettingsSearchFocusIds.APPEARANCE_HOME -> if (isTablet) 10 else 8
        else -> null
    }
}

internal fun resolvePlaybackSettingsScrollIndex(
    focusId: String
): Int? {
    return when (focusId) {
        SettingsSearchFocusIds.PLAYBACK_DECODER -> 0
        SettingsSearchFocusIds.PLAYBACK_SPEED -> 2
        SettingsSearchFocusIds.PLAYBACK_MINI_PLAYER -> 4
        SettingsSearchFocusIds.PLAYBACK_GESTURE -> 6
        SettingsSearchFocusIds.PLAYBACK_DEBUG -> 8
        SettingsSearchFocusIds.PLAYBACK_INTERACTION -> 10
        SettingsSearchFocusIds.PLAYBACK_NETWORK -> 12
        SettingsSearchFocusIds.PLAYBACK_DATA_SAVER -> 14
        SettingsSearchFocusIds.PLAYBACK_FULLSCREEN -> 10
        else -> null
    }
}

internal fun resolveBottomBarSettingsScrollIndex(
    focusId: String
): Int? {
    return when (focusId) {
        SettingsSearchFocusIds.BOTTOM_BAR_DISPLAY -> 1
        SettingsSearchFocusIds.BOTTOM_BAR_TOP_TABS -> 3
        SettingsSearchFocusIds.BOTTOM_BAR_CURRENT -> 5
        SettingsSearchFocusIds.BOTTOM_BAR_AVAILABLE -> 7
        else -> null
    }
}
