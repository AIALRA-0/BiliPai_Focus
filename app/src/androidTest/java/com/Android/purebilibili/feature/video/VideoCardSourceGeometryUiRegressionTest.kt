package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.videoCardSourceBoundsInRoot
import com.android.purebilibili.feature.video.screen.resolveVideoDetailReturnMediaLayoutFrame
import com.android.purebilibili.feature.video.screen.resolveVideoDetailReturnSourceCardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoCardSourceGeometryUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clippedCardsKeepTheirCompleteCoverRatioAndReturnEndpoint() {
        val ratio = mutableFloatStateOf(16f / 9f)
        var cardCoordinates: LayoutCoordinates? = null
        var coverCoordinates: LayoutCoordinates? = null
        var infoCoordinates: LayoutCoordinates? = null
        rule.setContent {
            // Both ends of the card extend beyond the visible scrolling viewport.
            Box(Modifier.size(240.dp, 80.dp).clipToBounds()) {
                Column(
                    Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .width(240.dp)
                        .offset(y = (-20).dp)
                        .onGloballyPositioned { cardCoordinates = it },
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(ratio.floatValue)
                        .onGloballyPositioned { coverCoordinates = it })
                    Box(Modifier.fillMaxWidth().height(40.dp)
                        .onGloballyPositioned { infoCoordinates = it })
                }
            }
        }
        for (aspectRatio in listOf(16f / 9f, 4f / 3f, 16f / 10f)) {
            rule.runOnIdle { ratio.floatValue = aspectRatio }
            rule.runOnIdle {
                val card = requireNotNull(cardCoordinates).videoCardSourceBoundsInRoot()
                val cover = requireNotNull(coverCoordinates).videoCardSourceBoundsInRoot()
                val info = requireNotNull(infoCoordinates).videoCardSourceBoundsInRoot()
                val visibleCover = requireNotNull(coverCoordinates).boundsInRoot()
                assertTrue(visibleCover.height < cover.height)
                assertEquals(cover.width / aspectRatio, cover.height, 1f)
                assertEquals(cover.bottom, info.top, 1f)
                assertEquals(info.bottom, card.bottom, 1f)

                val landing = resolveVideoDetailReturnSourceCardLayout(
                    viewportWidthPx = 1080f,
                    sourceBounds = card,
                    sourceCoverBounds = cover,
                    sourceInfoBounds = info,
                    sourceLayout = VideoCardSourceLayout.STACKED,
                )
                assertTrue(landing.canRender)
                // A 16:9 player must land at the measured source ratio, including
                // cover pixels that were outside the viewport at click time.
                val frame = resolveVideoDetailReturnMediaLayoutFrame(
                    containerWidthPx = 1080,
                    containerHeightPx = 608,
                    landingLayout = landing,
                    handoffProgress = 1f,
                )
                assertEquals(cover.width, frame.widthPx * landing.sourceScale, 1f)
                assertEquals(cover.height, frame.heightPx * landing.sourceScale, 1f)
            }
        }
    }
}
