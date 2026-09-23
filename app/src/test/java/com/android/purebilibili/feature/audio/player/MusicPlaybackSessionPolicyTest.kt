package com.android.purebilibili.feature.audio.player

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlaybackSessionPolicyTest {

    @Test
    fun `single audio song disables unavailable queue controls`() {
        assertEquals(
            MusicQueueControlState(
                hasPrevious = false,
                hasNext = false,
                showQueue = true
            ),
            resolveMusicQueueControlState(queueSize = 1, currentIndex = 0)
        )
    }

    @Test
    fun `middle item enables both directions and queue`() {
        assertEquals(
            MusicQueueControlState(
                hasPrevious = true,
                hasNext = true,
                showQueue = true
            ),
            resolveMusicQueueControlState(queueSize = 3, currentIndex = 1)
        )
    }

    @Test
    fun `shuffle queue keeps next enabled at sequential boundary`() {
        assertEquals(
            MusicQueueControlState(
                hasPrevious = true,
                hasNext = true,
                showQueue = true
            ),
            resolveMusicQueueControlState(
                queueSize = 3,
                currentIndex = 2,
                playMode = com.android.purebilibili.feature.video.player.PlayMode.SHUFFLE
            )
        )
    }

    @Test
    fun `mini player managed session survives screen exit`() {
        assertFalse(shouldReleaseMusicPlayerOnScreenExit(isManagedByMiniPlayer = true))
        assertTrue(shouldReleaseMusicPlayerOnScreenExit(isManagedByMiniPlayer = false))
    }

    @Test
    fun `playback sources expose stable cache and media ids`() {
        val audio = MusicPlaybackSource.AudioSong(42L)
        val video = MusicPlaybackSource.VideoAudio("BV1abc", 7L, "Title")

        assertEquals("au:42", audio.stableId)
        assertEquals("video:BV1abc:7", video.stableId)
    }

    @Test
    fun `video audio remains owned by player view model`() {
        assertEquals(
            MusicPlaybackOwner.MINI_PLAYER_MANAGER,
            resolveMusicPlaybackOwner(MusicPlaybackSource.AudioSong(42L))
        )
        assertEquals(
            MusicPlaybackOwner.PLAYER_VIEW_MODEL,
            resolveMusicPlaybackOwner(MusicPlaybackSource.VideoAudio("BV1abc", 7L, "Title"))
        )

        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicDetailScreen.kt",
            "src/main/java/com/android/purebilibili/feature/audio/screen/MusicDetailScreen.kt"
        )
        assertTrue(screenSource.contains("AudioModeScreen("))
        assertFalse(screenSource.contains("loadMusicFromVideo("))
    }

    @Test
    fun `music view model delegates ownership to mini player manager`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/viewmodel/MusicViewModel.kt",
            "src/main/java/com/android/purebilibili/feature/audio/viewmodel/MusicViewModel.kt"
        )

        assertTrue(source.contains("MiniPlayerManager"))
        assertFalse(source.contains("exoPlayer?.release()"))
    }

    @Test
    fun `mini player manager exposes standalone audio entry point`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/player/MiniPlayerManager.kt",
            "src/main/java/com/android/purebilibili/feature/video/player/MiniPlayerManager.kt"
        )

        assertTrue(source.contains("fun startAudio("))
        assertTrue(source.contains("setMediaItem(mediaItem)"))
    }

    private fun loadSource(vararg candidates: String): String {
        return candidates.asSequence()
            .map(::File)
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Cannot locate source from ${candidates.toList()}")
    }
}
