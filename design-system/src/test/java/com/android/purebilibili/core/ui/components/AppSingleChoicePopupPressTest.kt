package com.android.purebilibili.core.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSingleChoicePopupPressTest {
    private class Receiver : AppSingleChoicePopupPress.Listener {
        var moves = 0
        var releases = 0
        var cancellations = 0
        override fun onMove(positionInWindow: Offset) { moves++ }
        override fun onRelease(positionInWindow: Offset) { releases++ }
        override fun onCancel() { cancellations++ }
    }

    @Test
    fun releasingAtTheSourceKeepsTheMenuWithoutPickingAnItem() {
        val receiver = Receiver()
        val press = AppSingleChoicePopupPress()
        press.attach(receiver)
        press.release(Offset.Zero)
        press.cancel()
        assertEquals(0, receiver.releases)
        assertEquals(1, receiver.cancellations)
    }

    @Test
    fun carriedReleaseIsDeliveredOnlyOnceAndDisposalDisconnectsIt() {
        val receiver = Receiver()
        val press = AppSingleChoicePopupPress()
        press.attach(receiver)
        press.move(Offset(10f, 20f))
        press.release(Offset(30f, 40f))
        press.release(Offset.Zero)
        press.cancel()
        assertEquals(1, receiver.moves)
        assertEquals(1, receiver.releases)
        assertEquals(0, receiver.cancellations)
        val detached = AppSingleChoicePopupPress()
        detached.attach(receiver)
        detached.attach(null)
        detached.move(Offset.Zero)
        detached.release(Offset.Zero)
        assertEquals(1, receiver.releases)
    }

    @Test
    fun cancelledPressCannotAttachToAnotherMenu() {
        val receiver = Receiver()
        val press = AppSingleChoicePopupPress()
        press.cancel()
        press.attach(receiver)
        press.move(Offset.Zero)
        press.release(Offset.Zero)
        assertEquals(0, receiver.moves)
        assertEquals(0, receiver.releases)
    }
}
