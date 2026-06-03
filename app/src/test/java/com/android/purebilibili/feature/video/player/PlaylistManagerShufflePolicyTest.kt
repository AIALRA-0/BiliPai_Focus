package com.android.purebilibili.feature.video.player

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistManagerShufflePolicyTest {

    @BeforeTest
    fun setUp() {
        PlaylistManager.clearPlaylist()
        PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)
    }

    @Test
    fun `shuffle should not repeat tracks before exhausting the current cycle`() {
        var progress = ShuffleProgress(
            history = listOf(0),
            historyIndex = 0,
            cyclePlayed = setOf(0)
        )

        val first = advanceShuffleProgress(
            playlistSize = 4,
            currentIndex = 0,
            progress = progress,
            chooseCandidate = { candidates -> candidates.last() }
        )
        assertEquals(3, first.nextIndex)

        progress = first.progress
        val second = advanceShuffleProgress(
            playlistSize = 4,
            currentIndex = 3,
            progress = progress,
            chooseCandidate = { candidates -> candidates.last() }
        )
        assertEquals(2, second.nextIndex)

        progress = second.progress
        val third = advanceShuffleProgress(
            playlistSize = 4,
            currentIndex = 2,
            progress = progress,
            chooseCandidate = { candidates -> candidates.last() }
        )
        assertEquals(1, third.nextIndex)
        assertEquals(setOf(0, 1, 2, 3), third.progress.cyclePlayed)
    }

    @Test
    fun `shuffle should start a new cycle only after all other tracks were played`() {
        val result = advanceShuffleProgress(
            playlistSize = 4,
            currentIndex = 1,
            progress = ShuffleProgress(
                history = listOf(0, 3, 2, 1),
                historyIndex = 3,
                cyclePlayed = setOf(0, 1, 2, 3)
            ),
            chooseCandidate = { candidates -> candidates.first() }
        )

        assertEquals(0, result.nextIndex)
        assertEquals(setOf(0, 1), result.progress.cyclePlayed)
    }

    @Test
    fun `playlist refresh should preserve shuffle progress for existing tracks`() {
        val previous = listOf(
            playlistItem("BV1"),
            playlistItem("BV2")
        )
        val refreshed = listOf(
            playlistItem("BV1"),
            playlistItem("BV2"),
            playlistItem("BV3"),
            playlistItem("BV4")
        )

        val progress = reconcileShuffleProgressForPlaylistUpdate(
            previousPlaylist = previous,
            newPlaylist = refreshed,
            currentIndex = 1,
            progress = ShuffleProgress(
                history = listOf(0, 1),
                historyIndex = 1,
                cyclePlayed = setOf(0, 1)
            )
        )

        assertEquals(listOf(0, 1), progress.history)
        assertEquals(1, progress.historyIndex)
        assertEquals(setOf(0, 1), progress.cyclePlayed)
    }

    @Test
    fun `playlist manager should keep shuffle traversal after queue refresh`() {
        PlaylistManager.setPlaylist(
            items = listOf(
                playlistItem("BV1"),
                playlistItem("BV2")
            ),
            startIndex = 0
        )
        PlaylistManager.setPlayMode(PlayMode.SHUFFLE)

        assertEquals("BV2", PlaylistManager.playNext()?.bvid)

        PlaylistManager.setPlaylist(
            items = listOf(
                playlistItem("BV1"),
                playlistItem("BV2"),
                playlistItem("BV3")
            ),
            startIndex = 1
        )

        assertEquals("BV1", PlaylistManager.playPrevious()?.bvid)
        assertEquals("BV2", PlaylistManager.playNext()?.bvid)
        assertEquals("BV3", PlaylistManager.playNext()?.bvid)
    }

    @Test
    fun `sequential mode should stop at queue end`() {
        PlaylistManager.setPlaylist(
            items = listOf(
                playlistItem("BV1"),
                playlistItem("BV2")
            ),
            startIndex = 1
        )
        PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)

        assertEquals(null, PlaylistManager.playNext())
    }

    @Test
    fun `repeat one mode should keep current item on next`() {
        PlaylistManager.setPlaylist(
            items = listOf(
                playlistItem("BV1"),
                playlistItem("BV2")
            ),
            startIndex = 1
        )
        PlaylistManager.setPlayMode(PlayMode.REPEAT_ONE)

        assertEquals("BV2", PlaylistManager.playNext()?.bvid)
        assertEquals(1, PlaylistManager.currentIndex.value)
    }

    private fun playlistItem(bvid: String) = PlaylistItem(
        bvid = bvid,
        title = bvid,
        cover = "",
        owner = ""
    )
}
