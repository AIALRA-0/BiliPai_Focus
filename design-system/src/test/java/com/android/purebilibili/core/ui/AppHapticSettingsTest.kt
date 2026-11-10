package com.android.purebilibili.core.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AppHapticSettingsTest {
    @Test
    fun existingUsersKeepEverySceneEnabledAndBitsDoNotOverlap() {
        val defaults = AppHapticSettings()
        assertTrue(AppHapticScene.entries.all(defaults::isEnabled))
        assertEquals(AppHapticScene.entries.size, AppHapticScene.entries.map { it.bit }.toSet().size)
        assertTrue(AppHapticScene.entries.all { it.bit > 0 && it.bit and (it.bit - 1) == 0 })
    }

    @Test
    fun togglingOneSceneKeepsOtherChoicesAndUnknownFutureBits() {
        val initial = AppHapticSettings(1 shl 20)
        val changed = initial.withEnabled(AppHapticScene.PLAYER_SEEK, false)
            .withEnabled(AppHapticScene.MENUS, false)
            .withEnabled(AppHapticScene.PLAYER_SEEK, true)
        assertTrue(changed.isEnabled(AppHapticScene.PLAYER_SEEK))
        assertFalse(changed.isEnabled(AppHapticScene.MENUS))
        assertTrue(changed.isEnabled(AppHapticScene.PLAYER_GESTURES))
        assertEquals((1 shl 20) or AppHapticScene.MENUS.bit, changed.disabledMask)
    }

    @Test
    fun scenePolicyReadsCurrentSettingsAndMasterWithoutRecreatingFeedback() {
        var count = 0
        val delegate = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { count++ }
        }
        var config = AppThemeConfig()
        val root = AppPolicyHapticFeedback(delegate, { config })
        val seek = root.forScene(AppHapticScene.PLAYER_SEEK)
        val gesture = root.forScene(AppHapticScene.PLAYER_GESTURES)
        seek.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        config = config.copy(hapticSettings = config.hapticSettings.withEnabled(AppHapticScene.PLAYER_SEEK, false))
        seek.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        gesture.performHapticFeedback(HapticFeedbackType.LongPress)
        config = config.copy(hapticFeedbackEnabled = false)
        gesture.performHapticFeedback(HapticFeedbackType.LongPress)
        config = config.copy(hapticFeedbackEnabled = true)
        seek.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        gesture.performHapticFeedback(HapticFeedbackType.LongPress)
        assertEquals(3, count)
        assertFalse(config.hapticSettings.isEnabled(AppHapticScene.PLAYER_SEEK))
    }

    @Test
    fun disabledSceneCannotLeakThroughGenericButtonFallback() {
        var count = 0
        val delegate = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { count++ }
        }
        val config = AppThemeConfig(hapticSettings = AppHapticSettings().withEnabled(AppHapticScene.VIDEO_ACTIONS, false))
        val root = AppPolicyHapticFeedback(delegate, { config })
        AppActionHaptics.runAction {
            root.forScene(AppHapticScene.VIDEO_ACTIONS).performHapticFeedback(HapticFeedbackType.LongPress)
            root.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        assertEquals(0, count)
        AppActionHaptics.runAction { root.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        assertEquals(1, count)
    }
}
