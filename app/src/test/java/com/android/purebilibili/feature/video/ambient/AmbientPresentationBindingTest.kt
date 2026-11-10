package com.android.purebilibili.feature.video.ambient

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AmbientPresentationBindingTest {
    @Test
    fun releasingBindingDropsViewCallbacksAndVisibility() {
        val presentation = AmbientPresentation()
        val owner = Any()
        presentation.claimBinding(owner)
        presentation.visible = true
        presentation.layoutEnabled = true
        presentation.videoBoundsInWindow = { Rect(0f, 0f, 100f, 100f) }

        assertTrue(presentation.releaseBinding(owner))

        assertNull(presentation.videoBoundsInWindow())
        assertFalse(presentation.visibilityGate())
        assertFalse(presentation.visible)
        assertFalse(presentation.layoutEnabled)
        assertFalse(presentation.releaseBinding(owner))
    }

    @Test
    fun outgoingBindingCannotClearNewOutputCallbacks() {
        val presentation = AmbientPresentation()
        val oldOwner = Any()
        val newOwner = Any()
        val newBounds = { Rect(0f, 0f, 200f, 100f) }
        presentation.claimBinding(oldOwner)
        presentation.claimBinding(newOwner)
        presentation.videoBoundsInWindow = newBounds
        presentation.visible = true

        assertFalse(presentation.releaseBinding(oldOwner))

        assertSame(newBounds, presentation.videoBoundsInWindow)
        assertTrue(presentation.visible)
        assertTrue(presentation.releaseBinding(newOwner))
    }
}
