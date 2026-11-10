package com.android.purebilibili.feature.video.player

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.lifecycle.BackgroundManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundPausedReleaseTest {
    @Test
    fun pauseInBackgroundReleasesAfterGracePeriodAndClearsResumeIntent() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            assertFalse(getField(fixture.manager, "foregroundResumeIntent") as Boolean)
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS - 1)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
            advanceTimeBy(1)
            runCurrent()
            verify(exactly = 1) { fixture.player.stop() }
            verify(atLeast = 1) { fixture.player.clearVideoSurface() }
            verify(exactly = 0) { fixture.player.clearMediaItems() }
            verify(exactly = 0) { fixture.player.play() }
        }
    }

    @Test
    fun resumingBeforeDeadlineCancelsIdleRelease() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            advanceTimeBy(10_000)
            fixture.wantsPlayback = true
            fixture.listener.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
        }
    }

    @Test
    fun resumeAfterIdleReleasePreparesMediaAndAllowsAnotherPauseRelease() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            fixture.wantsPlayback = true
            fixture.listener.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            verify(exactly = 1) { fixture.player.prepare() }
            fixture.playbackState = Player.STATE_READY
            fixture.wantsPlayback = false
            fixture.pause()
            runCurrent()
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 2) { fixture.player.stop() }
        }
    }

    @Test
    fun returningToForegroundCancelsIdleReleaseAndStaysPaused() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            every { BackgroundManager.isInBackground } returns false
            fixture.manager.onEnterForeground()
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
            verify(exactly = 0) { fixture.player.play() }
        }
    }

    @Test
    fun playerReplacementDoesNotStopDepartingOrNewPlayer() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            val replacement = mockk<ExoPlayer>(relaxed = true)
            setField(fixture.manager, "_externalPlayer", replacement)
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
            verify(exactly = 0) { replacement.stop() }
        }
    }

    @Test
    fun enteringPipDuringGracePeriodPreservesPausedVideo() = runTest {
        withManager { fixture ->
            fixture.pause()
            runCurrent()
            setState(fixture.manager, "isSystemPipActive", true)
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
        }
    }

    @Test
    fun audioFocusPauseDoesNotScheduleUserIdleRelease() = runTest {
        withManager { fixture ->
            fixture.listener.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
            advanceTimeBy(BACKGROUND_PAUSED_RELEASE_DELAY_MS)
            runCurrent()
            verify(exactly = 0) { fixture.player.stop() }
        }
    }

    private class Fixture(val manager: MiniPlayerManager, val player: ExoPlayer) {
        var wantsPlayback = false
        var playbackState = Player.STATE_READY
        val listener = getField(manager, "playerListener") as Player.Listener
        fun pause() = listener.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }

    private fun TestScope.withManager(block: (Fixture) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkStatic(ContextCompat::class)
        every { ContextCompat.registerReceiver(any(), any(), any(), any()) } returns null
        mockkObject(BackgroundManager)
        every { BackgroundManager.isInBackground } returns true
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<NotificationManager>(relaxed = true)
        every { context.startService(any()) } returns ComponentName("com.android.purebilibili", "PlaybackService")
        val constructor = MiniPlayerManager::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        val manager = constructor.newInstance(context)
        val player = mockk<ExoPlayer>(relaxed = true)
        val fixture = Fixture(manager, player)
        every { player.playWhenReady } answers { fixture.wantsPlayback }
        every { player.isPlaying } returns false
        every { player.mediaItemCount } returns 1
        every { player.playbackState } answers { fixture.playbackState }
        every { player.stop() } answers { fixture.playbackState = Player.STATE_IDLE }
        every { player.trackSelectionParameters } returns TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
        setField(manager, "_externalPlayer", player)
        setField(manager, "backgroundOptimizationPlayer", player)
        setField(manager, "isLowMemoryMode", true)
        setField(manager, "foregroundResumeIntent", true)
        setState(manager, "isActive", true)
        try {
            block(fixture)
        } finally {
            manager.release()
            BackgroundManager.removeListener(manager)
            unmockkAll()
            Dispatchers.resetMain()
        }
    }

    companion object {
        private fun getField(manager: MiniPlayerManager, name: String): Any? =
            MiniPlayerManager::class.java.getDeclaredField(name).apply { isAccessible = true }.get(manager)

        private fun setField(manager: MiniPlayerManager, name: String, value: Any) {
            MiniPlayerManager::class.java.getDeclaredField(name).apply { isAccessible = true }.set(manager, value)
        }

        @Suppress("UNCHECKED_CAST")
        private fun setState(manager: MiniPlayerManager, name: String, value: Boolean) {
            (getField(manager, "${name}\$delegate") as MutableState<Boolean>).value = value
        }
    }
}
