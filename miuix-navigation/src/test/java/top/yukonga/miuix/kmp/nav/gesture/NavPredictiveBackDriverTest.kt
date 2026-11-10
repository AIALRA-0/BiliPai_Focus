package top.yukonga.miuix.kmp.nav.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.nav.runtime.settleCancel
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge

class NavPredictiveBackDriverTest {
    @Test
    fun regrabInterruptsCancelWithoutResettingToRest() = runTest {
        val driver = Animatable(0.65f)
        val restore = launch(BroadcastFrameClock(), start = CoroutineStart.UNDISPATCHED) {
            driver.settleCancel(target = 1f, spec = NavMotion.Default.cancel)
        }
        val applied = mutableListOf<Float>()

        drivePredictiveBack(
            events = flowOf(event(0f), event(0.1f)),
            animatedTop = driver,
            topIndex = 1,
            onProgressApplied = { applied += it },
        )
        restore.join()

        assertTrue(restore.isCancelled)
        assertEquals(0.35f, applied[0], 0.0001f)
        assertEquals(0.45f, applied[1], 0.0001f)
        assertEquals(0.55f, driver.value, 0.0001f)
    }

    @Test
    fun reversingNewFingerTravelReturnsToGrabPosition() = runTest {
        val driver = Animatable(0.65f)
        val applied = mutableListOf<Float>()
        drivePredictiveBack(
            events = flowOf(event(0f), event(0.2f), event(0.05f), event(0f)),
            animatedTop = driver,
            topIndex = 1,
            onProgressApplied = { applied += it },
        )

        assertEquals(0.55f, applied[1], 0.0001f)
        assertEquals(0.4f, applied[2], 0.0001f)
        assertEquals(0.65f, driver.value, 0.0001f)
    }

    @Test
    fun supersededStreamCannotMoveOrPublishIntoNewGesture() = runTest {
        val driver = Animatable(0.65f)
        var active = true
        val applied = mutableListOf<Float>()
        drivePredictiveBack(
            events = flow {
                emit(event(0f))
                active = false
                // A newer gesture already owns this position before the old queued event arrives.
                driver.snapTo(0.8f)
                emit(event(0.4f))
            },
            animatedTop = driver,
            topIndex = 1,
            isActive = { active },
            onProgressApplied = { applied += it },
        )

        assertEquals(1, applied.size)
        assertEquals(0.8f, driver.value, 0.0001f)
    }

    private fun event(progress: Float) = NavBackEvent(
        progress = progress,
        swipeEdge = NavSwipeEdge.Left,
        touchY = 400f,
    )
}
