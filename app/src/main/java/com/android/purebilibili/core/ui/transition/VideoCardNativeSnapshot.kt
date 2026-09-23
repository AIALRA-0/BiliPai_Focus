package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.android.purebilibili.core.util.CardPositionManager
import kotlinx.coroutines.launch

internal const val VIDEO_CARD_FLYING_OVERLAY_COVER_DEPTH = 0.001f

/**
 * Click pre-arms OPENING before NavDisplay mounts the destination. Hide the list slot
 * only once that overlay is actually covering the source bounds, otherwise the click
 * flashes an empty card-shaped hole.
 */
internal fun isVideoCardFlyingOverlayCoveringSource(
    phase: VideoCardTransitionBackgroundPhase,
    depthProgress: Float,
    isReturnGestureInProgress: Boolean,
): Boolean {
    if (isReturnGestureInProgress) return true
    return when (phase) {
        VideoCardTransitionBackgroundPhase.OPENING,
        VideoCardTransitionBackgroundPhase.RETURNING,
        -> depthProgress > VIDEO_CARD_FLYING_OVERLAY_COVER_DEPTH
        VideoCardTransitionBackgroundPhase.HELD -> true
        VideoCardTransitionBackgroundPhase.IDLE -> false
    }
}

/**
 * The flying overlay owns the clicked card. The list slot must be empty while that
 * overlay covers the source, otherwise a frozen duplicate sits under the morph.
 */
internal fun shouldHideStationarySourceCard(
    isSharedMorphSourceCard: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    depthProgress: Float,
    isReturnGestureInProgress: Boolean,
): Boolean {
    if (!isSharedMorphSourceCard) return false
    return isVideoCardFlyingOverlayCoveringSource(
        phase = phase,
        depthProgress = depthProgress,
        isReturnGestureInProgress = isReturnGestureInProgress,
    )
}

internal fun isRecordedNativeCardSource(
    bvid: String,
    sourceRoute: String?,
    activeSourceKey: String?,
    recordedSourceKey: String? = CardPositionManager.lastClickedVideoSourceKey,
): Boolean {
    val clicked = recordedSourceKey ?: return false
    // A click updates the pending key before navigation replaces the previous HELD session.
    if (activeSourceKey != clicked) return false
    val id = bvid.trim()
    if (id.isEmpty()) return false
    val route = normalizeSharedElementSourceRoute(sourceRoute) ?: return false
    return clicked == "$route:$id"
}

internal fun isNativeVideoCardLayerDrawable(widthPx: Int, heightPx: Int): Boolean =
    widthPx > 1 && heightPx > 1

/**
 * Records the stationary list card into a graphics layer so a click can freeze native pixels
 * instead of reconstructing title/spacing on the flying detail entry.
 *
 * The recorded layer can be drawn directly for ordinary cards via hardware-accelerated drawLayer.
 * Sources that leave composition, such as the now-playing bar, additionally freeze this layer
 * to a stable bitmap at click time.
 * While the flying overlay covers this card, skip drawing at the list coordinates.
 */
@Composable
internal fun Modifier.recordNativeVideoCardLayer(
    layer: GraphicsLayer,
    freezeProvider: () -> Boolean,
    bvid: String = "",
    sourceRoute: String?,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val bgState = LocalVideoCardTransitionBackgroundState.current
    return drawWithContent {
        // Read the latch in draw. Waiting for recomposition after a click can otherwise
        // overwrite the frozen first-click card after OPENING has hidden its info band.
        if (!freezeProvider()) {
            layer.record {
                this@drawWithContent.drawContent()
            }
        }
        val hide = shouldHideStationarySourceCard(
            isSharedMorphSourceCard = isRecordedNativeCardSource(
                bvid = bvid,
                sourceRoute = sourceRoute,
                activeSourceKey = bgState.sourceKeyProvider(),
            ),
            phase = bgState.phaseProvider(),
            depthProgress = bgState.progressProvider(),
            isReturnGestureInProgress = bgState.isReturnGestureInProgressProvider() ||
                bgState.isGestureRestoreInProgressProvider(),
        )
        if (!hide) {
            if (freezeProvider() && isNativeVideoCardLayerDrawable(layer.size.width, layer.size.height)) {
                drawLayer(layer)
            } else {
                drawContent()
            }
        }
    }
}

internal fun captureNativeVideoCardImage(
    layer: GraphicsLayer,
) {
    CardPositionManager.recordNativeCardLayer(
        layer.takeIf {
            isNativeVideoCardLayerDrawable(it.size.width, it.size.height)
        },
    )
}

internal fun captureNativeCoverOverlayLayer(
    layer: GraphicsLayer,
) {
    CardPositionManager.recordNativeCoverOverlayLayer(
        layer.takeIf {
            isNativeVideoCardLayerDrawable(it.size.width, it.size.height)
        },
    )
}

internal suspend fun captureNativeVideoCardBitmap(
    layer: GraphicsLayer,
    expectedSourceKey: String?,
) {
    if (!isNativeVideoCardLayerDrawable(layer.size.width, layer.size.height)) return
    CardPositionManager.recordNativeCardBitmap(
        bitmap = layer.toImageBitmap(),
        expectedSourceKey = expectedSourceKey,
    )
}

@Composable
internal fun rememberNativeVideoCardLayer() = rememberGraphicsLayer()

internal class NativeVideoCardSnapshotController(
    val modifier: Modifier,
    val coverOverlayModifier: Modifier,
    val capture: () -> Unit,
    val freezeToBitmap: () -> Unit,
)

@Composable
internal fun rememberNativeVideoCardSnapshotController(
    key: Any,
    enabled: Boolean = true,
): NativeVideoCardSnapshotController {
    val layer = rememberNativeVideoCardLayer()
    val coverOverlayLayer = rememberNativeVideoCardLayer()
    val captureScope = rememberCoroutineScope()
    val freezeState = remember(key) { mutableStateOf(false) }
    val bvid = (key as? String).orEmpty()
    val sourceRoute = LocalVideoCardSharedElementSourceRoute.current
    return NativeVideoCardSnapshotController(
        modifier = Modifier.recordNativeVideoCardLayer(
            layer = layer,
            freezeProvider = {
                freezeState.value && CardPositionManager.isNativeVideoCardLayerCurrentOwner(layer)
            },
            bvid = bvid,
            sourceRoute = sourceRoute,
            enabled = enabled,
        ),
        coverOverlayModifier = Modifier.recordNativeVideoCardLayer(
            layer = coverOverlayLayer,
            freezeProvider = {
                freezeState.value &&
                    CardPositionManager.isNativeCoverOverlayLayerCurrentOwner(coverOverlayLayer)
            },
            bvid = bvid,
            sourceRoute = sourceRoute,
            enabled = enabled,
        ),
        capture = {
            freezeState.value = true
            captureNativeVideoCardImage(layer)
            captureNativeCoverOverlayLayer(coverOverlayLayer)
        },
        freezeToBitmap = {
            val expectedSourceKey = CardPositionManager.lastClickedVideoSourceKey
            captureScope.launch {
                captureNativeVideoCardBitmap(
                    layer = layer,
                    expectedSourceKey = expectedSourceKey,
                )
            }
        },
    )
}
