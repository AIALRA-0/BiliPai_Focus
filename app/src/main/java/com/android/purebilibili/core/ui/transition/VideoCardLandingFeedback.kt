package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A single outward release, overlapping the existing spring's return rather than queued after it. */
internal fun videoCardLandingReleaseSpec(releaseDp: Float): KeyframesSpec<Float> = keyframes {
    durationMillis = 300
    0f at 0
    0f at 100 using AppMotionEasing.M1Standard
    releaseDp at 190 using AppMotionEasing.M1Standard
    0f at 300
}

/**
 * Custom video morph feedback, shared across materials; native MD3/Miuix controls are untouched.
 * Keep the flying entry and the first source frame at 1x. Compress only after handoff, outside
 * native pixel recording, so another click never captures an already scaled card.
 * Cover-overlay recording must not opt in: the entire card already owns this transform.
 */
@Composable
internal fun Modifier.videoCardLandingFeedback(
    layer: GraphicsLayer,
    bvid: String,
    sourceRoute: String?,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val landingFeedback = LocalVideoCardTransitionClock.current?.landingFeedback ?: return this
    val background = rememberUpdatedState(LocalVideoCardTransitionBackgroundState.current)
    val motionEnabled = LocalComponentMotionEnabled.current && !LocalSystemReduceMotion.current
    if (!motionEnabled) return this
    // Ownership is a coarse boundary. Only the matching source allocates animation state,
    // collects the landing event and adds a feedback layer; neighbours observe no frames.
    val ownsSource = remember(bvid, sourceRoute) {
        derivedStateOf(structuralEqualityPolicy()) {
            isRecordedNativeCardSource(bvid, sourceRoute, background.value.sourceKeyProvider())
        }
    }
    if (!ownsSource.value) return this
    val density = LocalDensity.current.density
    // Animate physical edge travel, not a fixed percentage: wide cards must not visibly
    // shrink more than feed tiles. Read animated values only in the graphics layer.
    val edgeCompression = remember(layer, bvid, sourceRoute, density) { Animatable(0f, 0.015f) }
    val edgeRelease = remember(layer, bvid, sourceRoute, density) { Animatable(0f, 0.005f) }
    LaunchedEffect(layer, bvid, sourceRoute, density, motionEnabled, landingFeedback) {
        edgeCompression.snapTo(0f)
        edgeRelease.snapTo(0f)
        if (!motionEnabled) return@LaunchedEffect
        snapshotFlow {
            val state = background.value
            val sourceMatches = isRecordedNativeCardSource(
                bvid, sourceRoute, state.sourceKeyProvider(),
            )
            if (sourceMatches && state.motionTierProvider() == MotionTier.Normal) {
                // Unrelated cards observe source ownership only, never global phase changes.
                state.exposureProvider() to landingFeedback.event
            } else null
        }.collectLatest { frame ->
            edgeCompression.snapTo(0f)
            edgeRelease.snapTo(0f)
            val readyFrame = frame ?: return@collectLatest
            val event = readyFrame.second ?: return@collectLatest
            if (readyFrame.first != VideoCardTransitionExposure.Idle) return@collectLatest
            var compressionDp = 0f
            // A recreated card may receive the event before its native layer's first draw.
            // Wait for geometry without consuming the event or subscribing composition to size.
            for (attempt in 0..2) {
                compressionDp = resolveVideoCardLandingEdgeCompressionDp(
                    widthPx = layer.size.width.toFloat(),
                    heightPx = layer.size.height.toFloat(),
                    density = density,
                    sourceLayout = event.sourceLayout,
                )
                if (compressionDp > 0f) break
                withFrameNanos { }
            }
            if (compressionDp > 0f && landingFeedback.claim(event, background.value.sourceKeyProvider())) {
                val motion = resolveVideoCardLandingMotion(event.sourceLayout)
                val releaseDp = resolveVideoCardLandingReleaseDp(
                    widthPx = layer.size.width.toFloat(),
                    heightPx = layer.size.height.toFloat(),
                    density = density,
                    sourceLayout = event.sourceLayout,
                )
                // Start together on the frame clock. New input cancels both children, so the
                // outward response cannot survive its owning return or restart independently.
                coroutineScope {
                    launch {
                        edgeCompression.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = motion.dampingRatio,
                                stiffness = motion.stiffness,
                                visibilityThreshold = 0.015f,
                            ),
                            initialVelocity = resolveVideoCardLandingImpulseVelocity(compressionDp, event.sourceLayout),
                        )
                    }
                    launch {
                        edgeRelease.animateTo(
                            targetValue = 0f,
                            animationSpec = videoCardLandingReleaseSpec(releaseDp),
                        )
                    }
                }
            }
        }
    }
    return graphicsLayer {
        // Deferred read: no frame-rate recomposition, layout resizing, or anchor movement.
        val state = background.value
        val value = if (motionEnabled && state.motionTierProvider() == MotionTier.Normal &&
            state.exposureProvider() == VideoCardTransitionExposure.Idle
        ) resolveVideoCardLandingScale(
            widthPx = size.width,
            heightPx = size.height,
            density = density,
            // Ignore the original spring's tiny negative tail; exactly one bounded release
            // is authored by edgeRelease instead of amplifying late oscillations.
            edgeCompressionDp = edgeCompression.value.coerceAtLeast(0f) - edgeRelease.value,
            sourceLayout = landingFeedback.event?.sourceLayout ?: VideoCardSourceLayout.COVER_ONLY,
        ) else 1f
        scaleX = value
        scaleY = value
    }
}
