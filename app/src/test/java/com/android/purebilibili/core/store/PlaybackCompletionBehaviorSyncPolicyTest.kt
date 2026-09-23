package com.android.purebilibili.core.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCompletionBehaviorSyncPolicyTest {

    @Test
    fun syncPrefersMemoryCacheOverStaleSharedPreferences() {
        assertEquals(
            PlaybackCompletionBehavior.PLAY_IN_ORDER,
            resolvePlaybackCompletionBehaviorSyncSource(
                memoryCacheValue = PlaybackCompletionBehavior.PLAY_IN_ORDER.value,
                sharedPreferencesValue = PlaybackCompletionBehavior.REPEAT_ONE.value,
            )
        )
    }

    @Test
    fun syncFallsBackToSharedPreferencesWhenMemoryEmpty() {
        assertEquals(
            PlaybackCompletionBehavior.REPEAT_ONE,
            resolvePlaybackCompletionBehaviorSyncSource(
                memoryCacheValue = null,
                sharedPreferencesValue = PlaybackCompletionBehavior.REPEAT_ONE.value,
            )
        )
    }

    @Test
    fun syncDefaultsWhenNeitherSourceExists() {
        assertEquals(
            PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
            resolvePlaybackCompletionBehaviorSyncSource(
                memoryCacheValue = null,
                sharedPreferencesValue = null,
            )
        )
    }

    @Test
    fun sharedPreferencesShouldHealWhenDisagreeingWithDataStore() {
        assertTrue(
            shouldHealPlaybackCompletionSharedPreferences(
                dataStoreValue = PlaybackCompletionBehavior.PLAY_IN_ORDER.value,
                sharedPreferencesValue = PlaybackCompletionBehavior.REPEAT_ONE.value,
            )
        )
        assertFalse(
            shouldHealPlaybackCompletionSharedPreferences(
                dataStoreValue = PlaybackCompletionBehavior.PLAY_IN_ORDER.value,
                sharedPreferencesValue = PlaybackCompletionBehavior.PLAY_IN_ORDER.value,
            )
        )
    }
}
