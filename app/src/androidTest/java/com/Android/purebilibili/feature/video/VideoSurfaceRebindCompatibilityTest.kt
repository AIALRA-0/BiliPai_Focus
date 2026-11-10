@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.video

import android.net.Uri
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.feature.video.ui.section.rebindPlayerSurfaceIfNeeded
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the API 28 route with real player output; device-specific codec stalls need API 28 hardware. */
@RunWith(AndroidJUnit4::class)
class VideoSurfaceRebindCompatibilityTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var player: ExoPlayer
    private lateinit var firstView: PlayerView
    private lateinit var secondView: PlayerView
    private val renderedOutput = AtomicReference<Any?>()

    @Test
    fun repeatedApi28NativeRebindKeepsTheRenderedFrameVisible() = withPlayback {
        rule.runOnIdle {
            val shutter = firstView.findViewById<View>(androidx.media3.ui.R.id.exo_shutter)
            assertEquals(View.INVISIBLE, shutter.visibility)
            repeat(4) {
                rebindPlayerSurfaceIfNeeded(firstView, player, sdkInt = 28)
                // Detaching PlayerView.player closes the shutter until another first frame arrives.
                assertEquals(View.INVISIBLE, shutter.visibility)
            }
        }
    }

    @Test
    fun api28RetainedViewReclaimsOutputAfterAnotherViewTakesOver() = withPlayback {
        val firstSurface = rule.runOnIdle { nativeSurface(firstView) }
        val secondSurface = rule.runOnIdle {
            renderedOutput.set(null)
            rebindPlayerSurfaceIfNeeded(secondView, player, sdkInt = 28)
            nativeSurface(secondView)
        }
        rule.waitUntil(timeoutMillis = 15_000) { renderedOutput.get() === secondSurface }

        // firstView still references this player, but the decoder output belongs to secondView.
        rule.runOnIdle {
            renderedOutput.set(null)
            rebindPlayerSurfaceIfNeeded(firstView, player, sdkInt = 28)
        }
        rule.waitUntil(timeoutMillis = 15_000) { renderedOutput.get() === firstSurface }
    }

    private fun nativeSurface(view: PlayerView): Surface =
        (view.videoSurfaceView as SurfaceView).holder.surface

    private fun withPlayback(assertPlayback: () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("api28-rebind-", ".mp4", context.cacheDir)
        try {
            instrumentation.context.assets.open("api28_surface_rebind.mp4").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).build().apply {
                    addAnalyticsListener(object : AnalyticsListener {
                        override fun onRenderedFirstFrame(
                            eventTime: AnalyticsListener.EventTime,
                            output: Any,
                            renderTimeMs: Long,
                        ) {
                            renderedOutput.set(output)
                        }
                    })
                    repeatMode = Player.REPEAT_MODE_ONE
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    playWhenReady = true
                    prepare()
                }
            }
            val playbackPlayer = player
            rule.setContent {
                Column {
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = false
                                player = playbackPlayer
                                firstView = this
                            }
                        },
                        modifier = Modifier.size(160.dp, 90.dp),
                    )
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = false
                                secondView = this
                            }
                        },
                        modifier = Modifier.size(160.dp, 90.dp),
                    )
                }
            }
            val firstSurface = rule.runOnIdle { nativeSurface(firstView) }
            rule.waitUntil(timeoutMillis = 15_000) { renderedOutput.get() === firstSurface }
            assertPlayback()
        } finally {
            instrumentation.runOnMainSync {
                if (::firstView.isInitialized) firstView.player = null
                if (::secondView.isInitialized) secondView.player = null
                if (::player.isInitialized) player.release()
            }
            file.delete()
        }
    }
}
