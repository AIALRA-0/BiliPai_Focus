package com.android.purebilibili.core.store

import com.android.purebilibili.core.store.player.PlayerSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerSettingsStoreParityTest {

    @Test
    fun `player store preferred speed resolution stays aligned`() {
        assertEquals(
            resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = true,
                lastSpeed = 1.8f
            ),
            PlayerSettingsStore.resolvePreferredPlaybackSpeed(
                defaultSpeed = 1.3f,
                rememberLastSpeed = true,
                lastSpeed = 1.8f
            )
        )
    }

    @Test
    fun `player store playback speed normalization stays aligned`() {
        assertEquals(
            normalizePlaybackSpeed(0.0f),
            PlayerSettingsStore.normalizePlaybackSpeed(0.0f)
        )
        assertEquals(
            normalizePlaybackSpeed(9.5f),
            PlayerSettingsStore.normalizePlaybackSpeed(9.5f)
        )
    }

    @Test
    fun `player volume is normalized to two percent steps`() {
        assertEquals(0f, PlayerSettingsStore.normalizePlayerVolume(-0.2f))
        assertEquals(0.02f, PlayerSettingsStore.normalizePlayerVolume(0.011f))
        assertEquals(0.48f, PlayerSettingsStore.normalizePlayerVolume(0.489f))
        assertEquals(1f, PlayerSettingsStore.normalizePlayerVolume(1.2f))
    }

    @Test
    fun `player insight mode prefers stored value and migrates legacy switch`() {
        assertEquals(
            PlayerSettingsStore.PlayerInsightMode.OFF,
            PlayerSettingsStore.resolvePlayerInsightMode("OFF", true, true)
        )
        assertEquals(
            PlayerSettingsStore.PlayerInsightMode.ALWAYS,
            PlayerSettingsStore.resolvePlayerInsightMode(null, true, true)
        )
        assertEquals(
            PlayerSettingsStore.PlayerInsightMode.OFF,
            PlayerSettingsStore.resolvePlayerInsightMode(null, true, false)
        )
        assertEquals(
            PlayerSettingsStore.PlayerInsightMode.OFF,
            PlayerSettingsStore.resolvePlayerInsightMode("UNKNOWN", false, false)
        )
        assertEquals(
            PlayerSettingsStore.PlayerInsightMode.OFF,
            PlayerSettingsStore.resolvePlayerInsightMode(null, false, false)
        )
    }
}
