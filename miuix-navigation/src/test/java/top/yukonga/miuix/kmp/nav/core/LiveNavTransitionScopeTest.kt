package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.runtime.NavPresentation
import top.yukonga.miuix.kmp.nav.transition.NavRole

class LiveNavTransitionScopeTest {
    @Test
    fun removingSignalIsAvailableBeforeReturnAnimationStarts() {
        val presentation = NavPresentation(1f)
        val scope = LiveNavTransitionScope(
            presentation = presentation,
            entryIndex = 1,
            isRemoving = true,
            change = NavChange.Pop,
            layoutSize = IntSize(1080, 2400),
            layoutDirection = LayoutDirection.Ltr,
            density = Density(3f),
        )
        assertEquals(NavRole.Top, scope.role)
        assertEquals(false, scope.isRunning)
        assertEquals(true, scope.isRemoving)
    }

    @Test
    fun roleReadersObserveBoundariesWhileDepthRemainsLive() = runTest {
        val presentation = NavPresentation(.2f)
        val scope = LiveNavTransitionScope(
            presentation = presentation,
            entryIndex = 1,
            isRemoving = true,
            change = NavChange.Pop,
            layoutSize = IntSize(1080, 2400),
            layoutDirection = LayoutDirection.Ltr,
            density = Density(3f),
        )
        val observer = SnapshotStateObserver { it() }
        val reader = Any()
        var invalidations = 0
        val onChanged: (Any) -> Unit = { invalidations++ }
        fun observe() = observer.observeReads(reader, onChanged) { scope.role }
        Snapshot.sendApplyNotifications()
        observer.start()
        try {
            observe()
            for (top in listOf(.4f, .6f, .8f)) {
                presentation.animatedTop.snapTo(top)
                Snapshot.sendApplyNotifications()
                assertEquals(0, invalidations)
                assertEquals(NavRole.Outgoing, scope.role)
                assertEquals(top - 1f, scope.relativeDepth, .0001f)
                observe()
            }
            presentation.animatedTop.snapTo(1f)
            Snapshot.sendApplyNotifications()
            assertEquals(1, invalidations)
            assertEquals(NavRole.Top, scope.role)
            observe()
            presentation.animatedTop.snapTo(1.2f)
            Snapshot.sendApplyNotifications()
            assertEquals(2, invalidations)
            assertEquals(NavRole.Covered, scope.role)
        } finally {
            observer.stop()
            observer.clear()
        }
    }
}
