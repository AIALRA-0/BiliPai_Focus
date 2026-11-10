package com.android.purebilibili.feature.video

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.feature.video.screen.VideoDetailReturnCoverImage
import com.android.purebilibili.feature.video.screen.resolveVideoDetailReturnSourceCardLayout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoDetailReturnCoverDrawingUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tallerCoverPaintsBelowThePlayerViewportThroughReturnAndCancellation() {
        val ratio = mutableFloatStateOf(4f / 3f)
        val progress = mutableFloatStateOf(0f)
        val decoded = mutableIntStateOf(0)
        // Deterministic local image; blue must fill the complete cover, including its lower edge.
        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
        rule.setContent {
            val context = LocalContext.current
            val widthPx = with(LocalDensity.current) { 240.dp.toPx() }
            val request = remember(context) {
                ImageRequest.Builder(context).data(bitmap).size(400, 300).crossfade(false).build()
            }
            val landing = remember(ratio.floatValue, widthPx) {
                resolveVideoDetailReturnSourceCardLayout(
                    viewportWidthPx = widthPx,
                    sourceBounds = Rect(0f, 0f, widthPx, widthPx / ratio.floatValue + widthPx / 6f),
                    sourceCoverBounds = Rect(0f, 0f, widthPx, widthPx / ratio.floatValue),
                    sourceLayout = VideoCardSourceLayout.STACKED,
                )
            }
            Box(Modifier.size(240.dp, 240.dp).background(Color.Red).testTag("cover-host")) {
                VideoDetailReturnCoverImage(
                    request = request,
                    landingLayout = landing,
                    handoffProgressProvider = { progress.floatValue },
                    inverseScaleXProvider = { 1f },
                    inverseScaleYProvider = { 1f },
                    alphaProvider = { 0.5f },
                    backingColor = Color.Black,
                    onDecoded = { decoded.intValue++ },
                    modifier = Modifier.width(240.dp).height(135.dp),
                )
            }
        }
        rule.waitUntil(timeoutMillis = 5_000) { decoded.intValue > 0 }
        for (coverRatio in listOf(4f / 3f, 16f / 10f, 16f / 9f)) {
            rule.runOnIdle { ratio.floatValue = coverRatio }
            // Reversing the same geometry models a cancelled and restarted return.
            for (handoff in listOf(1f, 0.5f, 0f, 0.5f, 1f)) {
                rule.runOnIdle { progress.floatValue = handoff }
                val pixels = rule.onNodeWithTag("cover-host").captureToImage().toPixelMap()
                val density = pixels.width / 240f
                val height = 135f + (240f / coverRatio - 135f) * handoff
                val lowerEdge = pixels[pixels.width / 2, ((height - 4f) * density).toInt()]
                assertTrue("Missing cover below player: ratio=$coverRatio progress=$handoff", lowerEdge.blue > 0.4f)
                assertTrue("Unpainted cover shows background: ratio=$coverRatio progress=$handoff", lowerEdge.red < 0.7f)
            }
        }
    }
}
