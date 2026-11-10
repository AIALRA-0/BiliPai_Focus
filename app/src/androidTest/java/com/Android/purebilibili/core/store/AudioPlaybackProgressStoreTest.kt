package com.android.purebilibili.core.store

import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioPlaybackProgressStoreTest {
    @Test
    fun checkpointsSeparateAccountsAndPartsAndKeepLatestSample() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bvid = "BVprogress_${System.nanoTime()}"
        val now = System.currentTimeMillis()
        val first = AudioPlaybackCheckpoint(11L, bvid, 101L, 1_200_000L, now)
        val second = first.copy(positionMs = 1_800_000L, savedAtMs = now + 1L)
        AudioPlaybackProgressStore.save(context, second)
        AudioPlaybackProgressStore.save(context, first)
        assertEquals(second, AudioPlaybackProgressStore.read(context, 11L, bvid, 101L))
        val raw = MultiProcessPreferences.get(context, "audio_playback_progress").data.first()[
            stringPreferencesKey("11:$bvid:101")
        ]
        assertEquals(second, Json.decodeFromString<AudioPlaybackCheckpoint>(requireNotNull(raw)))
        assertNull(AudioPlaybackProgressStore.read(context, 22L, bvid, 101L))
        assertNull(AudioPlaybackProgressStore.read(context, 11L, bvid, 202L))

        val restart = second.copy(positionMs = 0L, savedAtMs = now + 2L)
        AudioPlaybackProgressStore.save(context, restart)
        assertEquals(0L, AudioPlaybackProgressStore.read(context, 11L, bvid, 101L)?.positionMs)

        val pending = restart.copy(positionMs = 25_000L, savedAtMs = now + 3L)
        AudioPlaybackProgressStore.capture(pending)
        assertEquals(pending, AudioPlaybackProgressStore.read(context, 11L, bvid, 101L))
    }
}
