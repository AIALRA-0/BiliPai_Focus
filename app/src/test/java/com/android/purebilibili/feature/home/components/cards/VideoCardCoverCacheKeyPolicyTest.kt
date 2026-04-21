package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VideoCardCoverCacheKeyPolicyTest {

    @Test
    fun `cache key keeps using bvid when present`() {
        val key = resolveVideoCardCoverCacheKey(
            video = VideoItem(
                bvid = "BV1ab411",
                pic = "https://example.com/a.jpg"
            ),
            useLowQualityCover = false
        )

        assertEquals("cover_BV1ab411_n", key)
    }

    @Test
    fun `cache key falls back to cover url for history live items without bvid`() {
        val first = resolveVideoCardCoverCacheKey(
            video = VideoItem(
                id = 11L,
                pic = "https://example.com/live-1.jpg",
                title = "live one"
            ),
            useLowQualityCover = false
        )
        val second = resolveVideoCardCoverCacheKey(
            video = VideoItem(
                id = 12L,
                pic = "https://example.com/live-2.jpg",
                title = "live two"
            ),
            useLowQualityCover = false
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `cache key still stays stable when both bvid and pic are blank`() {
        val key = resolveVideoCardCoverCacheKey(
            video = VideoItem(
                id = 99L,
                cid = 777L,
                title = "fallback"
            ),
            useLowQualityCover = true
        )

        assertEquals("cover_fallback_99_777_${"fallback".hashCode()}_s", key)
    }
}
