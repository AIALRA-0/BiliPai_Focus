@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.profile

import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test

class ProfileSkinVideoRenderingTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun videoFillsBothEdgesThroughBackdropCaptureResizeAndScroll() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        var player: ExoPlayer? = null
        val file = File.createTempFile("profile-video-", ".mp4", context.cacheDir)
        val firstFrame = AtomicBoolean()
        val captured = mutableStateOf(false)
        val tall = mutableStateOf(true)
        val scrolled = mutableStateOf(false)
        var bounds = Rect.Zero
        try {
            instrumentation.context.assets.open("api28_surface_rebind.mp4").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).build().apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() { firstFrame.set(true) }
                    })
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    prepare()
                    play()
                }
            }
            rule.setContent {
                val navigationSource = rememberChromeBackdropSource()
                val headerSource = rememberChromeBackdropSource()
                val density = LocalDensity.current
                val motion = ProfileBackgroundMotion(
                    scrollOffsetPx = { if (scrolled.value) with(density) { 40.dp.toPx() } else 0f },
                    pullOffsetPx = { 0f },
                    avatarShiftYPx = { if (scrolled.value) -with(density) { 40.dp.toPx() } else 0f },
                )
                Box(Modifier.fillMaxSize().then(if (captured.value) navigationSource.modifier else Modifier)) {
                    Box(Modifier.fillMaxSize().then(if (captured.value) headerSource.modifier else Modifier)) {
                        CompositionLocalProvider(LocalProfileBackgroundMotion provides motion) {
                            Box(Modifier.fillMaxWidth()
                                .profileHeaderBackgroundMotion(if (tall.value) 280.dp else 120.dp)
                                .clipToBounds().background(androidx.compose.ui.graphics.Color.Red)
                                .onGloballyPositioned { bounds = it.boundsInWindow() }) {
                                AndroidView(
                                    factory = { ProfileSkinVideoView(it).apply { this.player = player } },
                                    onRelease = { it.player = null },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            rule.waitUntil(15_000) { firstFrame.get() }
            for (capture in listOf(false, true)) {
                rule.runOnIdle { captured.value = capture }
                for (portrait in listOf(true, false, true)) {
                    rule.runOnIdle { tall.value = portrait }
                    for (scroll in listOf(false, true, false)) {
                        rule.runOnIdle { scrolled.value = scroll }
                        rule.waitUntil(
                            conditionDescription = "Video edges filled: capture=$capture tall=$portrait scroll=$scroll",
                            timeoutMillis = 10_000,
                        ) {
                            val screenshot = instrumentation.uiAutomation.takeScreenshot()
                            try {
                                val filled = listOf(bounds.left + 4, bounds.center.x, bounds.right - 4).all { x ->
                                    val pixel = screenshot.getPixel(x.toInt(), bounds.center.y.toInt())
                                    Color.blue(pixel) > 100 && Color.red(pixel) < 80
                                }
                                filled
                            } finally { screenshot.recycle() }
                        }
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync { player?.release() }
            file.delete()
        }
    }
}
