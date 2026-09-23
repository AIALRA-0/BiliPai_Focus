package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CompletableDeferred
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** A source and its recording modifier; attach [modifier] to exactly one content container. */
@Stable
internal class ChromeBackdropSource(
    val backdrop: LayerBackdrop,
    val modifier: Modifier,
    private val recorded: State<Boolean>,
) {
    val isReady: Boolean get() = recorded.value
}

/**
 * Miuix's source draws its children to the screen, then invokes onDraw to record them again.
 * Record the children once and let both destinations replay the same display list instead.
 * This matters for nested home/navigation sources and the dock's hidden glass/label capture.
 *
 * This is not a bitmap snapshot or a frame cache: child invalidations still record fresh
 * content. Keep the replay layer separate from the backdrop layer to avoid recording itself.
 * No RenderEffect, alpha or clipping is applied to the replay layer, so it does not request
 * another offscreen raster pass. Miuix still owns coordinates and backdrop lifecycle.
 */
@Composable
internal fun rememberChromeBackdropSource(): ChromeBackdropSource {
    val contentLayer = rememberGraphicsLayer()
    val recorded = remember(contentLayer) { mutableStateOf(false) }
    val firstRecording = remember(contentLayer) { CompletableDeferred<Unit>() }
    LaunchedEffect(firstRecording) {
        firstRecording.await()
        recorded.value = true
    }
    val backdrop = rememberLayerBackdrop(onDraw = {
        drawLayer(contentLayer)
        // Publish once, after the source has actually recorded; never guess using a delay.
        firstRecording.complete(Unit)
    })
    return remember(backdrop, contentLayer) {
        ChromeBackdropSource(
            backdrop = backdrop,
            recorded = recorded,
            modifier = Modifier
                .layerBackdrop(backdrop)
                .drawWithContent {
                    contentLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(contentLayer)
                },
        )
    }
}
