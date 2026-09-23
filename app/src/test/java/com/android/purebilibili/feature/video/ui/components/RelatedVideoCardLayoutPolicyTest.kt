package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class RelatedVideoCardLayoutPolicyTest {

    @Test
    fun `narrow half opened pane stacks cover above metadata`() {
        assertEquals(
            RelatedVideoCardLayout.STACKED,
            resolveRelatedVideoCardLayout(319),
        )
    }

    @Test
    fun `boundary width keeps horizontal card`() {
        assertEquals(
            RelatedVideoCardLayout.HORIZONTAL,
            resolveRelatedVideoCardLayout(320),
        )
        assertEquals(
            RelatedVideoCardLayout.HORIZONTAL,
            resolveRelatedVideoCardLayout(480),
        )
    }
}
