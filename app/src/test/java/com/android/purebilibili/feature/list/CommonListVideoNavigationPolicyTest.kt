package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommonListVideoNavigationPolicyTest {

    @Test
    fun `video navigation uses bvid when available`() {
        val request = resolveCommonListVideoNavigationRequest(
            video = VideoItem(
                bvid = "BV1xx",
                cid = 123L,
                pic = "https://example.com/cover.jpg"
            )
        )

        assertNotNull(request)
        assertEquals("BV1xx", request.lookupKey)
        assertEquals("BV1xx", request.bvid)
        assertEquals(123L, request.cid)
        assertEquals(false, request.isVertical)
    }

    @Test
    fun `video navigation preserves vertical metadata`() {
        val request = resolveCommonListVideoNavigationRequest(
            video = VideoItem(
                bvid = "BV1vertical",
                cid = 123L,
                pic = "https://example.com/cover.jpg",
                isVertical = true
            )
        )

        assertNotNull(request)
        assertEquals(true, request.isVertical)
    }

    @Test
    fun `history navigation falls back to render key when bvid is blank`() {
        val request = resolveCommonListVideoNavigationRequest(
            video = VideoItem(
                cid = 456L,
                pic = "https://example.com/live.jpg"
            ),
            fallbackLookupKey = "live_9988"
        )

        assertNotNull(request)
        assertEquals("live_9988", request.lookupKey)
        assertEquals("", request.bvid)
        assertEquals(456L, request.cid)
    }

    @Test
    fun `navigation request stays null when both bvid and fallback key are blank`() {
        val request = resolveCommonListVideoNavigationRequest(
            video = VideoItem(),
            fallbackLookupKey = " "
        )

        assertNull(request)
    }
}
