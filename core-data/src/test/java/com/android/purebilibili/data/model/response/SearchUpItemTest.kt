package com.android.purebilibili.data.model.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.serialization.json.Json

class SearchUpItemTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun liveSearchFieldsAcceptNumericAndStringValuesAndSurviveCleanup() {
        listOf(
            """{"mid":123,"is_live":1,"room_id":456}""",
            """{"mid":"123","is_live":"1","room_id":"456"}""",
        ).forEach { payload ->
            val up = json.decodeFromString(SearchUpItem.serializer(), payload).cleanupFields()
            assertEquals(1, up.is_live)
            assertEquals(456L, up.room_id)
        }
    }

    @Test
    fun unknownLiveStatusStaysDistinctFromOfflineAndReplay() {
        val missing = json.decodeFromString(SearchUpItem.serializer(), """{"mid":123}""")
        val explicitNull = json.decodeFromString(
            SearchUpItem.serializer(), """{"mid":123,"is_live":null,"room_id":null}"""
        )
        assertNull(missing.is_live)
        assertNull(missing.room_id)
        assertNull(explicitNull.is_live)
        assertNull(explicitNull.room_id)
        listOf(0, 2).forEach { status ->
            val up = json.decodeFromString(
                SearchUpItem.serializer(), """{"is_live":$status,"room_id":456}"""
            ).cleanupFields()
            assertEquals(status, up.is_live)
        }
    }

    @Test
    fun relatedVideosInheritMissingAuthorFromUpResult() {
        val up = SearchUpItem(
            mid = 123L,
            uname = "<em>录像分析员</em>",
            res = listOf(SearchVideoItem(bvid = "BV123", author = " "))
        ).cleanupFields()

        val video = up.res!!.single().toVideoItem()
        assertEquals("录像分析员", video.owner.name)
        assertEquals(123L, video.owner.mid)
    }

    @Test
    fun relatedVideosPreserveExistingAuthorInformation() {
        val up = SearchUpItem(
            mid = 123L,
            uname = "录像分析员",
            res = listOf(SearchVideoItem(author = "原作者", mid = 456L))
        ).cleanupFields()

        val video = up.res!!.single().toVideoItem()
        assertEquals("原作者", video.owner.name)
        assertEquals(456L, video.owner.mid)
    }
}
