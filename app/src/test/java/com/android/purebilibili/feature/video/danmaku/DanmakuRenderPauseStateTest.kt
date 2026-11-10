package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuRenderPauseStateTest {
    @Test
    fun repeatedPauseIsIdempotentAndResumePreservesUserPreference() {
        val state = DanmakuRenderPauseState<Any>()
        val view = Any()
        assertTrue(state.setPaused(view, true))
        assertFalse(state.setPaused(view, true))
        assertFalse(state.isRenderingEnabled(true, view))
        assertTrue(state.setPaused(view, false))
        assertFalse(state.setPaused(view, false))
        assertTrue(state.isRenderingEnabled(true, view))
        assertFalse(state.isRenderingEnabled(false, view))
    }

    @Test
    fun oldTargetPauseCannotSuppressReplacementTarget() {
        val state = DanmakuRenderPauseState<Any>()
        val oldView = Any()
        val newView = Any()
        state.setPaused(oldView, true)
        assertTrue(state.isRenderingEnabled(true, newView))
        state.setPaused(newView, true)
        state.setPaused(oldView, false)
        assertFalse(state.isRenderingEnabled(true, newView))
    }

    @Test
    fun detachingStaleTargetDoesNotResumeCurrentTarget() {
        val state = DanmakuRenderPauseState<Any>()
        val oldView = Any()
        val newView = Any()
        state.setPaused(oldView, true)
        state.setPaused(newView, true)
        state.detach(oldView)
        assertTrue(state.isRenderingEnabled(true, oldView))
        assertFalse(state.isRenderingEnabled(true, newView))
        state.detach(newView)
        assertTrue(state.isRenderingEnabled(true, newView))
    }

    @Test
    fun sessionReleaseClearsPausesWithoutEnablingUserDisabledDanmaku() {
        val state = DanmakuRenderPauseState<Any>()
        val view = Any()
        state.setPaused(view, true)
        state.clear()
        assertTrue(state.isRenderingEnabled(true, view))
        assertTrue(state.isRenderingEnabled(true, null))
        assertFalse(state.isRenderingEnabled(false, view))
    }
}
