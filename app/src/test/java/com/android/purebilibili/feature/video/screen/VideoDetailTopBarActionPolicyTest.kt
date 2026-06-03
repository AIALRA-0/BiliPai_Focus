package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailTopBarActionPolicyTest {

    @Test
    fun backButton_resolvesBackAction() {
        assertEquals(
            VideoDetailTopBarAction.BACK,
            resolveVideoDetailTopBarAction(isHomeButton = false)
        )
    }

    @Test
    fun homeButton_resolvesHomeAction() {
        assertEquals(
            VideoDetailTopBarAction.HOME,
            resolveVideoDetailTopBarAction(isHomeButton = true)
        )
    }
}
