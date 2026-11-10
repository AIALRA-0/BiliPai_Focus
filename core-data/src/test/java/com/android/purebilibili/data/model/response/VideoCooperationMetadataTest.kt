package com.android.purebilibili.data.model.response

import com.android.purebilibili.data.repository.asVideoItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCooperationMetadataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun popularRightsSurviveConversionAndPersistence() {
        val item = json.decodeFromString<PopularItem>(
            """{"aid":1,"rights":{"is_cooperation":1}}""",
        ).toVideoItem()
        assertTrue(item.isCooperation)
        assertTrue(json.decodeFromString<VideoItem>(json.encodeToString(item)).isCooperation)
    }

    @Test
    fun searchUnionFlagSurvivesConversion() {
        val item = json.decodeFromString<SearchVideoItem>(
            """{"is_union_video":1}""",
        ).toVideoItem()
        assertTrue(item.isCooperation)
    }

    @Test
    fun spaceBooleanAndIntegerVariantsSurviveConversion() {
        listOf(
            """{"is_cooperation":true}""",
            """{"is_cooperation":1}""",
            """{"is_union_video":1}""",
        ).forEach { payload ->
            assertTrue(json.decodeFromString<SpaceVideoItem>(payload).asVideoItem(1L).isCooperation)
        }
    }

    @Test
    fun aggregateSpaceCardsAcceptBooleanFlag() {
        assertTrue(json.decodeFromString<SpaceAggregateArchiveItem>(
            """{"is_cooperation":true}""",
        ).isCooperation)
    }

    @Test
    fun missingOrFalseFlagsDoNotCreateBadges() {
        listOf("{}", """{"is_cooperation":false,"is_union_video":0}""").forEach { payload ->
            assertFalse(json.decodeFromString<VideoItem>(payload).isCooperation)
        }
    }
}
