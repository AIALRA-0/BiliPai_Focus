package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HomeCategoryPageKeyPolicyTest {

    @Test
    fun `video grid key disambiguates duplicate bvids`() {
        val first = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 100L, aid = 100L, bvid = "BV1SEorB6E6u"),
            index = 0
        )
        val duplicate = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 100L, aid = 100L, bvid = "BV1SEorB6E6u"),
            index = 1
        )

        assertNotEquals(first, duplicate)
    }

    @Test
    fun `video grid key keeps bvid as primary identity`() {
        val key = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 100L, aid = 200L, bvid = "BV1SEorB6E6u"),
            index = 3
        )

        assertEquals("home_video_BV1SEorB6E6u_3", key)
    }

    @Test
    fun `video grid key falls back when bvid is blank`() {
        val key = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 42L, aid = 77L),
            index = 5
        )

        assertEquals("home_video_42_77_5", key)
    }
}
