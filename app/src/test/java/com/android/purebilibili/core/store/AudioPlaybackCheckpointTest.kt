package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioPlaybackCheckpointTest {
    private val previous = AudioPlaybackCheckpoint(12L, "BVcheckpoint", 34L, 1_200_000L, 100L)

    @Test
    fun lateOldSaveDoesNotRewindNewerProgress() {
        val latest = previous.copy(positionMs = 1_800_000L, savedAtMs = 200L)
        assertEquals(latest, selectLatestAudioCheckpoint(latest, previous))
    }

    @Test
    fun explicitRewindAndRestartReplaceOlderLargerPosition() {
        val rewind = previous.copy(positionMs = 60_000L, savedAtMs = 200L)
        val restart = previous.copy(positionMs = 0L, savedAtMs = 300L)
        assertEquals(rewind, selectLatestAudioCheckpoint(previous, rewind))
        assertEquals(restart, selectLatestAudioCheckpoint(rewind, restart))
    }
}
