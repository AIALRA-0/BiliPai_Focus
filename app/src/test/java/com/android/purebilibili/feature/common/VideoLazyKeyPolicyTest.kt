package com.android.purebilibili.feature.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VideoLazyKeyPolicyTest {

    @Test
    fun `indexed video lazy key disambiguates duplicate bvids`() {
        val first = resolveIndexedVideoLazyKey(
            namespace = "video_related",
            index = 0,
            bvid = "BV1M9DaBPER1",
            aid = 100L,
            cid = 200L
        )
        val duplicate = resolveIndexedVideoLazyKey(
            namespace = "video_related",
            index = 1,
            bvid = "BV1M9DaBPER1",
            aid = 100L,
            cid = 200L
        )

        assertNotEquals(first, duplicate)
    }

    @Test
    fun `indexed video lazy key keeps bvid as primary identity`() {
        val key = resolveIndexedVideoLazyKey(
            namespace = "category_video",
            index = 3,
            bvid = " BV1M9DaBPER1 ",
            id = 10L,
            aid = 20L,
            cid = 30L
        )

        assertEquals("category_video_BV1M9DaBPER1_3", key)
    }

    @Test
    fun `indexed video lazy key falls back to numeric identity`() {
        val key = resolveIndexedVideoLazyKey(
            namespace = "watch_later_video",
            index = 2,
            bvid = "",
            id = 0L,
            aid = 42L,
            cid = 77L
        )

        assertEquals("watch_later_video_aid_42_2", key)
    }
}
