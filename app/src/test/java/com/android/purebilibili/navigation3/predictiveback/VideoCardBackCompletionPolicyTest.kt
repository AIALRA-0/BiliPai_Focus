package com.android.purebilibili.navigation3.predictiveback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardBackCompletionPolicyTest {
    @Test
    fun `floating card lands after rotating away from its furthest position`() {
        assertTrue(VideoCardBackCompletionPolicy.shouldCommit(0.50f, 0.95f, -3f))
        assertTrue(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.50f, 0.95f))
    }

    @Test
    fun `explicit expansion back toward detail cancels even with neutral release`() {
        assertFalse(VideoCardBackCompletionPolicy.shouldCommit(0.30f, 0.95f, 0f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.30f, 0.95f))
    }

    @Test
    fun `strong reverse motion before floating mode retains original behavior`() {
        assertFalse(VideoCardBackCompletionPolicy.shouldCommit(0.40f, 0.45f, -2f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.40f, 0.45f))
    }

    @Test
    fun `neutral vertical lift respects platform completion before floating mode`() {
        assertTrue(VideoCardBackCompletionPolicy.shouldCommit(0.20f, 0.20f, 0f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.20f, 0.20f))
    }

    @Test
    fun `floating mode has separate entry and explicit restore boundaries`() {
        assertTrue(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.65f, 0.65f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.64f, 0.64f))
        assertTrue(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.36f, 0.95f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.35f, 0.95f))
    }

    @Test
    fun `new gesture does not inherit previous floating mode`() {
        assertTrue(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.50f, 0.95f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.50f, 0.50f))
    }

    @Test
    fun `invalid samples cannot cause an unintended pop`() {
        assertFalse(VideoCardBackCompletionPolicy.shouldCommit(Float.NaN, 0.95f, 0f))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommit(0.50f, 0.95f, Float.NaN))
        assertFalse(VideoCardBackCompletionPolicy.shouldCommitOnCancel(0.50f, Float.NaN))
    }
}
