package com.android.purebilibili.feature.video.playback.coordinator

import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.playback.session.PlaybackSessionStore
import com.android.purebilibili.feature.video.viewmodel.PlaybackEndAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackCoordinatorTest {

    @Test
    fun `resolvePlaybackEnded should store the selected completion action`() {
        val store = PlaybackSessionStore()
        val coordinator = PlaybackCoordinator(store)

        val action = coordinator.resolvePlaybackEnded(
            behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
            autoPlayEnabled = false,
            isExternalPlaylist = true,
            externalPlaylistAutoContinueEnabled = true,
            externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
            playMode = PlayMode.SEQUENTIAL
        )

        assertEquals(PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST, action)
        assertEquals(action, store.state.value.lastCompletionAction)
    }

    @Test
    fun `resolvePlaybackEnded should auto continue only when auto mode has page or season target`() {
        val store = PlaybackSessionStore()
        val coordinator = PlaybackCoordinator(store)

        val action = coordinator.resolvePlaybackEnded(
            behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
            autoPlayEnabled = false,
            isExternalPlaylist = false,
            externalPlaylistAutoContinueEnabled = false,
            externalPlaylistSource = ExternalPlaylistSource.NONE,
            playMode = PlayMode.SEQUENTIAL,
            hasNextPageOrSeasonTarget = true
        )

        assertEquals(PlaybackEndAction.AUTO_CONTINUE, action)
        assertEquals(action, store.state.value.lastCompletionAction)
    }

    @Test
    fun `refreshResumeSuggestion should store suggestion and mark prompt as shown`() {
        val store = PlaybackSessionStore()
        val coordinator = PlaybackCoordinator(store)
        val markedPromptKeys = mutableListOf<String>()
        val info = ViewInfo(
            bvid = "BV1multi",
            cid = 101L,
            pages = listOf(
                Page(cid = 101L, page = 1, part = "P1"),
                Page(cid = 205L, page = 5, part = "P5")
            )
        )

        val suggestion = coordinator.refreshResumeSuggestion(
            requestCid = 101L,
            loadedInfo = info,
            promptEnabled = true,
            hasPromptedBefore = { false },
            markPromptShown = { key -> markedPromptKeys += key },
            progressLookup = { bvid, cid ->
                when ("$bvid#$cid") {
                    "BV1multi#205" -> 15 * 60 * 1000L
                    else -> 0L
                }
            }
        )

        assertEquals(suggestion, store.state.value.resumeSuggestion)
        assertEquals(listOf("BV1multi#205"), markedPromptKeys)
    }

    @Test
    fun `refreshResumeSuggestion should clear state when prompt is disabled`() {
        val store = PlaybackSessionStore()
        val coordinator = PlaybackCoordinator(store)
        val info = ViewInfo(
            bvid = "BV1multi",
            cid = 101L,
            pages = listOf(
                Page(cid = 101L, page = 1, part = "P1"),
                Page(cid = 205L, page = 5, part = "P5")
            )
        )

        coordinator.refreshResumeSuggestion(
            requestCid = 101L,
            loadedInfo = info,
            promptEnabled = false,
            hasPromptedBefore = { false },
            markPromptShown = { error("prompt should not be marked when disabled") },
            progressLookup = { bvid, cid ->
                when ("$bvid#$cid") {
                    "BV1multi#205" -> 15 * 60 * 1000L
                    else -> 0L
                }
            }
        )

        assertNull(store.state.value.resumeSuggestion)
    }

    @Test
    fun `executePlaybackEndAction should hide dialog for stop`() {
        val coordinator = PlaybackCoordinator(PlaybackSessionStore())

        val outcome = coordinator.executePlaybackEndAction(
            action = PlaybackEndAction.STOP,
            repeatCurrent = { error("repeat should not run for stop") },
            playNextInOrder = { _ -> error("next should not run for stop") },
            playNextFromPlaylistLoop = { _ -> error("playlist loop should not run for stop") },
            autoContinue = { _ -> error("auto continue should not run for stop") }
        )

        assertTrue(outcome.shouldHidePlaybackEndedDialog)
    }

    @Test
    fun `executePlaybackEndAction should repeat current immediately`() {
        val coordinator = PlaybackCoordinator(PlaybackSessionStore())
        var repeated = false

        val outcome = coordinator.executePlaybackEndAction(
            action = PlaybackEndAction.REPEAT_CURRENT,
            repeatCurrent = { repeated = true },
            playNextInOrder = { _ -> false },
            playNextFromPlaylistLoop = { _ -> false },
            autoContinue = { _ -> }
        )

        assertTrue(repeated)
        assertFalse(outcome.shouldHidePlaybackEndedDialog)
    }

    @Test
    fun `executePlaybackEndAction should hide dialog when next in order cannot continue`() {
        val coordinator = PlaybackCoordinator(PlaybackSessionStore())

        val outcome = coordinator.executePlaybackEndAction(
            action = PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            repeatCurrent = {},
            playNextInOrder = { _ -> false },
            playNextFromPlaylistLoop = { _ -> true },
            autoContinue = { _ -> }
        )

        assertTrue(outcome.shouldHidePlaybackEndedDialog)
    }

    @Test
    fun `executePlaybackEndAction should auto continue without hiding dialog`() {
        val coordinator = PlaybackCoordinator(PlaybackSessionStore())
        var autoContinued = false
        var ignoredSavedProgress = false

        val outcome = coordinator.executePlaybackEndAction(
            action = PlaybackEndAction.AUTO_CONTINUE,
            repeatCurrent = {},
            playNextInOrder = { false },
            playNextFromPlaylistLoop = { false },
            autoContinue = { ignoreSavedProgress ->
                autoContinued = true
                ignoredSavedProgress = ignoreSavedProgress
            }
        )

        assertTrue(autoContinued)
        assertTrue(ignoredSavedProgress)
        assertFalse(outcome.shouldHidePlaybackEndedDialog)
    }
}
