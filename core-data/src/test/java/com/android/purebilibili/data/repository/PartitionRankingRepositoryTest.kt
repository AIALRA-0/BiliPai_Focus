package com.android.purebilibili.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PartitionRankingRepositoryTest {
    @Test fun animeAndOtherSeasonResponsesKeepSeasonIdentityAndCounts() {
        for (wrapper in listOf("result", "data")) {
            val payload = Json.parseToJsonElement("""{"code":0,"$wrapper":{"list":[{
                "season_id":"42","title":"剧集标题","cover":"https://example.com/cover.jpg",
                "new_ep":{"index_show":"更新至第3集"},"stat":{"view":1234,"follow":56}
            }]}}""").jsonObject
            val item = decodePartitionSeasonRanking(payload).single()
            assertEquals(42L, item.id)
            assertEquals("pgc_rank", item.contentType)
            assertEquals("https://www.bilibili.com/bangumi/play/ss42", item.navigationUrl)
            assertEquals("更新至第3集", item.owner.name)
            assertEquals(1234, item.stat.view)
            assertEquals(56, item.stat.favorite)
        }
    }

    @Test fun failedOrMissingPayloadDoesNotBecomeAnEmptyRanking() {
        for (payload in listOf("""{"code":-400,"message":"请求失败"}""", """{"code":0}""", "{}")) {
            assertFailsWith<IllegalStateException> {
                decodePartitionSeasonRanking(Json.parseToJsonElement(payload).jsonObject)
            }
        }
        assertEquals(emptyList(), decodePartitionSeasonRanking(
            Json.parseToJsonElement("""{"code":0,"data":{"list":[]}}""").jsonObject,
        ))
    }
}
