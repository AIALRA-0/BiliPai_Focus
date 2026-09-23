package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoComposerDraftStateTest {

    @Test
    fun `root and reply comments use separate draft keys`() {
        assertEquals(0L, commentComposerDraftKey(null))
        assertEquals(42L, commentComposerDraftKey(42L))
    }

    @Test
    fun `default draft state belongs to no persisted video`() {
        assertEquals("", VideoComposerDraftState().videoId)
        assertEquals("", VideoComposerDraftState().danmaku.text)
        assertEquals(emptyMap(), VideoComposerDraftState().comments)
    }
}
