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
            duplicateOrdinal = 0
        )
        val duplicate = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 100L, aid = 100L, bvid = "BV1SEorB6E6u"),
            duplicateOrdinal = 1
        )

        assertNotEquals(first, duplicate)
    }

    @Test
    fun `video grid key keeps bvid as primary identity`() {
        val key = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 100L, aid = 200L, bvid = "BV1SEorB6E6u"),
            duplicateOrdinal = 3
        )

        assertEquals("home_video_BV1SEorB6E6u_3", key)
    }

    @Test
    fun `video grid key falls back when bvid is blank`() {
        val key = resolveHomeCategoryVideoGridKey(
            video = VideoItem(id = 42L, aid = 77L),
            duplicateOrdinal = 5
        )

        assertEquals("home_video_42_77_5", key)
    }

    @Test
    fun `video grid keys stay stable when unrelated leading items change`() {
        val first = VideoItem(id = 1L, aid = 1L, bvid = "BV_FIRST")
        val anchored = VideoItem(id = 2L, aid = 2L, bvid = "BV_ANCHORED")

        val before = resolveHomeCategoryVideoGridKeys(listOf(first, anchored))[1]
        val after = resolveHomeCategoryVideoGridKeys(listOf(anchored))[0]

        assertEquals(before, after)
    }

    @Test
    fun `video grid keys disambiguate only actual duplicate identities`() {
        val duplicate = VideoItem(id = 100L, aid = 100L, bvid = "BV_DUPLICATE")

        assertEquals(
            listOf("home_video_BV_DUPLICATE_0", "home_video_BV_DUPLICATE_1"),
            resolveHomeCategoryVideoGridKeys(listOf(duplicate, duplicate))
        )
    }

    @Test
    fun `hero carousel dedup key uses bvid before numeric ids`() {
        val key = resolveHomeHeroCarouselDedupKey(
            VideoItem(id = 42L, aid = 77L, bvid = "BV1SEorB6E6u")
        )

        assertEquals("bvid_BV1SEorB6E6u", key)
    }

    @Test
    fun `hero carousel dedup key falls back when bvid is blank`() {
        val key = resolveHomeHeroCarouselDedupKey(
            VideoItem(id = 42L, aid = 77L)
        )

        assertEquals("id_42", key)
    }
}
