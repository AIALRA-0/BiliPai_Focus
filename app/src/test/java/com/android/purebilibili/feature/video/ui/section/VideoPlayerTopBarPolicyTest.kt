package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlayerTopBarPolicyTest {

    @Test
    fun homeClick_prefersExplicitHomeCallback() {
        var result = ""
        val callback = resolveVideoPlayerOverlayHomeClick(
            onBack = { result = "back" },
            onHomeClick = { result = "home" }
        )

        callback()

        assertEquals("home", result)
    }

    @Test
    fun homeClick_fallsBackToBackWhenHomeCallbackMissing() {
        var result = ""
        val callback = resolveVideoPlayerOverlayHomeClick(
            onBack = { result = "back" },
            onHomeClick = null
        )

        callback()

        assertEquals("back", result)
    }
}
