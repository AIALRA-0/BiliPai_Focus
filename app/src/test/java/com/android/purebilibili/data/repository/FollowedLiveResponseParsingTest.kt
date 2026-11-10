package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.FollowedLiveResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FollowedLiveResponseParsingTest {
    @Test
    fun `direct pagination fields and live count survive response decoding`() {
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<FollowedLiveResponse>(
            """{"code":0,"data":{"pageSize":9,"totalPage":2,"live_count":16,"list":[]}}"""
        )
        val data = requireNotNull(response.data)
        assertEquals(16, data.livingNum)
        assertEquals(9, data.pageSize)
        assertEquals(2, data.totalPage)
        assertTrue(hasMoreFollowedLivePages(1, 9, data.totalPage))
        assertFalse(hasMoreFollowedLivePages(2, 7, data.totalPage))
    }
}
