package com.android.purebilibili.navigation3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState

/** One visual owner for cover-only video entry and return. Navigation still owns the stack. */
internal class OfficialVideoSharedBoundsController {
    internal enum class Phase { Opening, Returning }
    private data class FrozenSource(
        val session: VideoCardTransitionSession,
        val sourceEntryKey: BiliPaiNavKey?,
        val targetEntryKey: BiliPaiNavKey?,
    )
    private val previousSources = ArrayDeque<FrozenSource>()

    var session by mutableStateOf<VideoCardTransitionSession?>(null)
        private set
    var phase by mutableStateOf<Phase?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var sourceEntryKey by mutableStateOf<BiliPaiNavKey?>(null)
        private set
    var targetEntryKey by mutableStateOf<BiliPaiNavKey?>(null)
        private set

    fun beginOpening(
        next: VideoCardTransitionSession,
        sourceEntry: BiliPaiNavKey? = null,
    ) {
        if (next.cardBounds?.let { it.width > 1f && it.height > 1f } != true) {
            return
        }
        session?.takeIf { it.sourceKey != next.sourceKey || it.bvid != next.bvid }?.let {
            previousSources.addLast(FrozenSource(it, sourceEntryKey, targetEntryKey))
        }
        session = next
        sourceEntryKey = sourceEntry
        targetEntryKey = null
        progress = 0f
        phase = Phase.Opening
    }

    fun setTargetEntry(key: BiliPaiNavKey?) {
        targetEntryKey = key
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
            sourceEntryKey = previous.sourceEntryKey
            targetEntryKey = previous.targetEntryKey
            progress = 1f
            phase = null
        }
    }

    fun clear() {
        previousSources.clear()
        phase = null
        session = null
        sourceEntryKey = null
        targetEntryKey = null
        progress = 0f
    }
}

/** Keeps a platform video surface out of the static cover transition without pausing playback. */
internal val LocalOfficialVideoCoverTransitionActive = compositionLocalOf { false }

/** The same official visibility transition is consumed by the retained source and real detail. */
internal val LocalOfficialVideoSharedTransition = compositionLocalOf<Transition<Boolean>?> { null }
internal val LocalOfficialVideoSharedSession =
    compositionLocalOf<VideoCardTransitionSession?> { null }

internal fun nowPlayingSharedSourceRoute(sourceRoute: String?): String =
    "${sourceRoute.orEmpty()}#now-playing"

@Composable
internal fun rememberOfficialVideoSharedTransition(
    controller: OfficialVideoSharedBoundsController,
): Transition<Boolean>? {
    val session = controller.session ?: return null
    val phase = controller.phase
    val depth = controller.progress
    val state = remember(session) {
        SeekableTransitionState(phase != OfficialVideoSharedBoundsController.Phase.Opening)
    }
    LaunchedEffect(session, phase, depth) {
        when (phase) {
            OfficialVideoSharedBoundsController.Phase.Opening ->
                state.seekTo(depth, targetState = true)
            OfficialVideoSharedBoundsController.Phase.Returning ->
                state.seekTo(1f - depth, targetState = false)
            null -> state.snapTo(true)
        }
    }
    return rememberTransition(state, label = "video-card-real-shared-bounds")
}

@Composable
internal fun OfficialVideoNowPlayingSourceScope(
    controller: OfficialVideoSharedBoundsController,
    bvid: String,
    content: @Composable () -> Unit,
) {
    val transition = LocalOfficialVideoSharedTransition.current
    val ownsBarSource = controller.session?.bvid == bvid &&
        controller.session?.sourceChromeSnapshot?.isNowPlayingBar == true
    if (transition == null || !ownsBarSource) {
        content()
        return
    }
    transition.AnimatedVisibility(
        visible = { expanded -> !expanded },
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            content()
        }
    }
}
