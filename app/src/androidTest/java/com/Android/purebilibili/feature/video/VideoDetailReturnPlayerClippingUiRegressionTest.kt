package com.android.purebilibili.feature.video

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.feature.video.screen.resolveVideoDetailReturnSourceCardLayout
import com.android.purebilibili.feature.video.screen.videoDetailReturnPlayerLayout
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoDetailReturnPlayerClippingUiRegressionTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun horizontalCardKeepsMovingCornersButReleasesNativePlayerAtEntryAndCancelledReturn() {
        rule.runOnUiThread {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        val progress = mutableFloatStateOf(1f)
        val frameReady = AtomicBoolean(false)
        rule.setContent {
            val density = LocalDensity.current
            val landing = remember(density) {
                with(density) {
                    resolveVideoDetailReturnSourceCardLayout(
                        viewportWidthPx = 240.dp.toPx(),
                        sourceBounds = Rect(0f, 0f, 240.dp.toPx(), 72.dp.toPx()),
                        sourceCoverBounds = Rect(0f, 0f, 108.dp.toPx(), 72.dp.toPx()),
                        sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
                    )
                }
            }
            Box(Modifier.size(280.dp, 180.dp).background(Color.Red)) {
                Box(Modifier.offset(16.dp, 16.dp).size(240.dp, 135.dp).testTag("native-player")) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().videoDetailReturnPlayerLayout(
                            landingLayout = landing,
                            handoffProgressProvider = { progress.floatValue },
                            inverseScaleXProvider = { 1f },
                            inverseScaleYProvider = { 1f },
                            clipCornerDp = 16.dp,
                        ),
                        factory = { context ->
                            TextureView(context).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    private fun paint(texture: SurfaceTexture) {
                                        val surface = Surface(texture)
                                        try {
                                            val canvas = surface.lockCanvas(null)
                                            canvas.drawColor(android.graphics.Color.BLUE)
                                            surface.unlockCanvasAndPost(canvas)
                                        } finally {
                                            surface.release()
                                        }
                                    }
                                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) = paint(texture)
                                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = paint(texture)
                                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true
                                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { frameReady.set(true) }
                                }
                            }
                        },
                        update = { it.isOpaque = progress.floatValue == 0f },
                    )
                }
            }
        }
        rule.waitUntil(5_000) { frameReady.get() }
        assertCorners(expanded = false)
        rule.runOnIdle { progress.floatValue = 0f }
        assertCorners(expanded = true)
        // A predictive return installs the round clip again; cancellation must release it too.
        rule.runOnIdle { progress.floatValue = 0.5f }
        assertCorners(expanded = false)
        rule.runOnIdle { progress.floatValue = 0f }
        assertCorners(expanded = true)
    }

    private fun assertCorners(expanded: Boolean) {
        rule.waitForIdle()
        // Capture the window's player region, not the display's transient rotation screenshot.
        val pixels = rule.onNodeWithTag("native-player").captureToImage().toPixelMap()
        val left = 2
        val top = 2
        val right = pixels.width - 3
        val bottom = pixels.height - 3
        for ((x, y) in listOf(left to top, right to top, left to bottom, right to bottom)) {
            val color = pixels[x, y]
            assertTrue("Native player corner ($x,$y), expanded=$expanded: $color",
                if (expanded) color.blue > 0.9f && color.red < 0.1f
                else color.red > 0.9f && color.blue < 0.1f)
        }
        val center = pixels[pixels.width / 5, pixels.height / 5]
        assertTrue("The native surface must remain visible during the morph", center.blue > 0.9f)
    }
}
