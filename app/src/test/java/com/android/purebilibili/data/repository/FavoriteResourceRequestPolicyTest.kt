package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteResourceRequestPolicyTest {

    @Test
    fun `resource list request fills browser defaults and clamps pagination`() {
        val request = resolveFavoriteResourceRequestParams(
            pn = 0,
            ps = 40,
            keyword = null,
            order = null,
            type = 0,
            tid = 0,
            platform = "",
        )

        assertEquals(1, request.page)
        assertEquals(20, request.pageSize)
        assertEquals("", request.keyword)
        assertEquals("mtime", request.order)
        assertEquals(0, request.type)
        assertEquals(0, request.tid)
        assertEquals("web", request.platform)
    }
}
