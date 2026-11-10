package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleInteractionModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `like status is independent from total likes`() {
        val data = json.decodeFromString<ArticleInfoResponse>(
            """{"code":0,"data":{"like":0,"favorite":true,"stats":{"like":42,"reply":0}}}"""
        ).data!!
        assertFalse(data.like)
        assertTrue(data.favorite)
        assertEquals(42L, data.stats?.like)
        assertEquals(0L, data.stats?.reply)
        assertNull(data.stats?.favorite)
    }

    @Test
    fun `liked article can have a zero count and missing statistics stay unknown`() {
        val liked = json.decodeFromString<ArticleInfoData>("""{"like":1,"stats":{"like":0}}""")
        assertTrue(liked.like)
        assertEquals(0L, liked.stats?.like)
        assertNull(json.decodeFromString<ArticleInfoData>("{}").stats)
    }
}
