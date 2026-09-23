package com.android.purebilibili.navigation3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

/** One visual owner for cover-only video entry and return. Navigation still owns the stack. */
internal class OfficialVideoSharedBoundsController {
    internal enum class Phase { Opening, Returning }
    private data class FrozenSource(
        val session: VideoCardTransitionSession,
        val bitmap: ImageBitmap?,
        val layer: GraphicsLayer?,
        val destination: Rect?,
    )
    private val previousSources = ArrayDeque<FrozenSource>()

    var session by mutableStateOf<VideoCardTransitionSession?>(null)
        private set
    var phase by mutableStateOf<Phase?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var sourceBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    var sourceLayer: GraphicsLayer? = null
        private set
    var destinationBounds by mutableStateOf<Rect?>(null)
        private set

    fun beginOpening(next: VideoCardTransitionSession, destination: Rect? = null) {
        if (next.cardBounds?.let { it.width > 1f && it.height > 1f } != true) {
            return
        }
        session?.takeIf { it.sourceKey != next.sourceKey || it.bvid != next.bvid }?.let {
            previousSources.addLast(FrozenSource(it, sourceBitmap, sourceLayer, destinationBounds))
        }
        session = next
        sourceBitmap = CardPositionManager.lastClickedNativeCardBitmap
        sourceLayer = CardPositionManager.lastClickedNativeCardLayer
        destinationBounds = destination
        progress = 0f
        phase = Phase.Opening
    }

    suspend fun freezeSourceBitmap(expectedSourceKey: String?) {
        val layer = sourceLayer ?: return
        if (layer.size.width <= 1 || layer.size.height <= 1) return
        val bitmap = try {
            layer.toImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return
        }
        if (session?.sourceKey == expectedSourceKey) sourceBitmap = bitmap
    }

    fun beginReturning() {
        if (session == null || phase == Phase.Returning || phase == Phase.Opening) return
        progress = 1f
        phase = Phase.Returning
    }

    fun onNavigationFrame(
        depth: Float,
        settle: VideoCardTransitionSettleState?,
        gestureInProgress: Boolean,
    ) {
        if (session == null) return
        val value = depth.coerceIn(0f, 1f)
        if (phase == null && (gestureInProgress || settle == VideoCardTransitionSettleState.AutoReturn)) {
            beginReturning()
        }
        progress = value
        when (phase) {
            Phase.Opening -> when {
                settle == VideoCardTransitionSettleState.Idle && value <= 0.001f ->
                    restorePreviousOrClear()
                settle == VideoCardTransitionSettleState.Held && value >= 0.999f -> phase = null
            }
            Phase.Returning -> when {
                settle == VideoCardTransitionSettleState.Idle && value <= 0.001f ->
                    restorePreviousOrClear()
                settle == VideoCardTransitionSettleState.Held && value >= 0.999f && !gestureInProgress ->
                    phase = null // predictive back was cancelled
            }
            null -> Unit
        }
    }

    private fun restorePreviousOrClear() {
        val previous = if (previousSources.isEmpty()) null else previousSources.removeLast()
        if (previous == null) {
            clear()
        } else {
            session = previous.session
            sourceBitmap = previous.bitmap
            sourceLayer = previous.layer
            destinationBounds = previous.destination
            progress = 1f
            phase = null
        }
    }

    fun clear() {
        previousSources.clear()
        phase = null
        session = null
        sourceBitmap = null
        sourceLayer = null
        destinationBounds = null
        progress = 0f
    }
}

/** Keeps a platform video surface out of the static cover transition without pausing playback. */
internal val LocalOfficialVideoCoverTransitionActive = compositionLocalOf { false }

/**
 * The source is a click-time Compose snapshot, including the bottom now-playing bar when its
 * real composable leaves the tree. The destination owns an opaque moving surface, while the real
 * detail screen owns its cover, title, and player chrome during the handoff.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.OfficialVideoSharedBoundsOverlay(
    controller: OfficialVideoSharedBoundsController,
    durationMillis: Int,
    modifier: Modifier = Modifier,
) {
    val session = controller.session ?: return
    val phase = controller.phase ?: return
    val bounds = session.cardBounds ?: return
    val density = LocalDensity.current
    val clipShape = remember(session.sourceCornerDp) {
        RoundedCornerShape((session.sourceCornerDp ?: 28).dp)
    }
    val duration = durationMillis.coerceAtLeast(1)
    androidx.compose.runtime.key(session.sourceKey, phase) {
        val opening = phase == OfficialVideoSharedBoundsController.Phase.Opening
        val transitionState = remember { SeekableTransitionState(!opening) }
        LaunchedEffect(controller.progress, opening) {
            transitionState.seekTo(
                fraction = if (opening) controller.progress else 1f - controller.progress,
                targetState = opening,
            )
        }
        val transition = rememberTransition(transitionState, label = "official-video-shared-bounds")
        transition.AnimatedContent(
            // sharedBounds owns the only content fade. Fading AnimatedContent as well makes the
            // source and destination translucent at the same time and exposes the retained page.
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            modifier = modifier.fillMaxSize(),
        ) { expanded ->
            Box(Modifier.fillMaxSize()) {
                val sharedKey = session.sourceKey ?: "video:${session.bvid}"
                if (expanded) {
                    val destination = controller.destinationBounds
                    Box(
                        Modifier.then(
                            if (destination != null) {
                                Modifier
                                    .offset {
                                        IntOffset(destination.left.roundToInt(), destination.top.roundToInt())
                                    }
                            } else Modifier
                        )
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(sharedKey),
                                animatedVisibilityScope = this@AnimatedContent,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                                boundsTransform = { _, _ -> tween(duration, easing = LinearEasing) },
                                zIndexInOverlay = 0f,
                                clipInOverlayDuringTransition = OverlayClip(clipShape),
                            )
                            .then(
                                if (destination != null) {
                                    Modifier.size(
                                        with(density) { destination.width.toDp() },
                                        with(density) { destination.height.toDp() },
                                    )
                                } else Modifier.fillMaxSize()
                            )
                            .background(AppSurfaceTokens.surface().copy(alpha = 1f)),
                    )
                } else {
                    val sourceModifier = Modifier
                        .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(sharedKey),
                            animatedVisibilityScope = this@AnimatedContent,
                            enter = androidx.compose.animation.fadeIn(
                                tween((duration * 0.55f).roundToInt().coerceAtLeast(1),
                                    easing = LinearEasing),
                            ),
                            exit = fadeOut(
                                tween((duration * 0.55f).roundToInt().coerceAtLeast(1),
                                    easing = LinearEasing),
                            ),
                            boundsTransform = { _, _ -> tween(duration, easing = LinearEasing) },
                            zIndexInOverlay = 1f,
                            clipInOverlayDuringTransition = OverlayClip(clipShape),
                        )
                        .size(
                            with(density) { bounds.width.toDp() },
                            with(density) { bounds.height.toDp() },
                        )
                        .clip(clipShape)
                    OfficialVideoSourceSnapshot(
                        session = session,
                        bitmap = controller.sourceBitmap ?: CardPositionManager.lastClickedNativeCardBitmap,
                        layer = controller.sourceLayer,
                        modifier = sourceModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialVideoSourceSnapshot(
    session: VideoCardTransitionSession,
    bitmap: ImageBitmap?,
    layer: GraphicsLayer?,
    modifier: Modifier = Modifier,
) {
    val snapshot = session.sourceChromeSnapshot
    val cover = snapshot?.coverUrl?.ifBlank { null } ?: session.coverIdentity
    when {
        bitmap != null -> Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier,
        )
        layer != null -> Canvas(modifier) { drawLayer(layer) }
        session.sourceLayout == VideoCardSourceLayout.STACKED -> {
            Column(modifier.background(AppSurfaceTokens.cardContainer())) {
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                }
                Column(Modifier.padding(8.dp)) {
                    AppText(snapshot?.title.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    snapshot?.ownerName?.takeIf { it.isNotBlank() }?.let {
                        AppText(it, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        else -> {
            Row(modifier.background(AppSurfaceTokens.cardContainer())) {
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(8.dp)) {
                    AppText(snapshot?.title.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    snapshot?.ownerName?.takeIf { it.isNotBlank() }?.let {
                        AppText(it, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
