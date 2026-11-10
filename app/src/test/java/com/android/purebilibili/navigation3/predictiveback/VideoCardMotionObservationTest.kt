package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import kotlin.test.Test
import kotlin.test.assertEquals
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

class VideoCardMotionObservationTest {
    private class Scope : NavTransitionScope {
        override var relativeDepth by mutableFloatStateOf(-.2f)
        override val role get() = if (relativeDepth < 0f) NavRole.Outgoing else NavRole.Top
        override val change = NavChange.Pop
        override val layoutSize = IntSize(1080, 2400)
        override val layoutDirection = LayoutDirection.Ltr
        override val density = Density(3f)
        override var gesture: NavGesture? by mutableStateOf(null)
        override var settle: NavSettle? by mutableStateOf(null)
    }

    @Test
    fun coarseReadersIgnoreMotionFramesButObserveSeekCancelAndCompletion() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress()
        progress.bind(scope)
        Snapshot.sendApplyNotifications()
        val observer = SnapshotStateObserver { it() }
        val reader = Any()
        var invalidations = 0
        val onChanged: (Any) -> Unit = { invalidations++ }
        fun observe() = observer.observeReads(reader, onChanged) {
            progress.isGestureInProgress()
            progress.settleStateOrNull()
        }
        observer.start()
        try {
            observe()
            for (depth in listOf(-.3f, -.5f, -.7f)) {
                Snapshot.withMutableSnapshot { scope.relativeDepth = depth }
                assertEquals(0, invalidations)
                observe()
            }
            Snapshot.withMutableSnapshot { scope.gesture = NavGesture(.2f, NavSwipeEdge.Left, 500f) }
            assertEquals(1, invalidations)
            assertEquals(VideoCardTransitionSettleState.InteractiveSeek, progress.settleStateOrNull())
            observe()
            for (fraction in listOf(.3f, .4f, .5f)) {
                Snapshot.withMutableSnapshot {
                    scope.relativeDepth = -fraction
                    scope.gesture = NavGesture(fraction, NavSwipeEdge.Left, 500f + fraction)
                }
                assertEquals(1, invalidations)
                observe()
            }
            Snapshot.withMutableSnapshot {
                scope.settle = object : NavSettle {
                    override val phase = NavSettlePhase.Cancel
                    override val releaseVelocity = 0f
                    override val elapsedMillis = 0f
                }
            }
            assertEquals(2, invalidations)
            assertEquals(VideoCardTransitionSettleState.CancelRestore, progress.settleStateOrNull())
            observe()
            Snapshot.withMutableSnapshot { scope.relativeDepth = -.1f }
            assertEquals(2, invalidations)
            observe()
            Snapshot.withMutableSnapshot {
                scope.relativeDepth = 0f
                scope.gesture = null
                scope.settle = null
            }
            assertEquals(3, invalidations)
            assertEquals(VideoCardTransitionSettleState.Held, progress.settleStateOrNull())
            observe()
            Snapshot.withMutableSnapshot { progress.clear() }
            assertEquals(4, invalidations)
        } finally {
            observer.stop()
            observer.clear()
        }
    }
}
