package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsImageUrlPolicyTest {

    @Test
    fun resolveVideoCoverUrl_returnsOriginalImageForHighQualityMode() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg",
            FormatUtils.resolveVideoCoverUrl(
                "//i0.hdslb.com/bfs/archive/demo.jpg",
                useLowQuality = false
            )
        )
    }

    @Test
    fun resolveVideoCoverUrl_usesSizedImageForLowQualityMode() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg@640w_400h.webp",
            FormatUtils.resolveVideoCoverUrl(
                "//i0.hdslb.com/bfs/archive/demo.jpg",
                useLowQuality = true
            )
        )
    }

    @Test
    fun buildSizedImageUrl_normalizes_protocol_and_applies_requested_size() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg@1080w_320h.webp",
            FormatUtils.buildSizedImageUrl("//i0.hdslb.com/bfs/archive/demo.jpg", width = 1080, height = 320)
        )
    }

    @Test
    fun buildSizedImageUrl_replaces_existing_resize_suffix() {
        assertEquals(
            "https://i0.hdslb.com/bfs/archive/demo.jpg@480w_300h.webp",
            FormatUtils.buildSizedImageUrl(
                "https://i0.hdslb.com/bfs/archive/demo.jpg@640w_400h.webp",
                width = 480,
                height = 300
            )
        )
    }
}
