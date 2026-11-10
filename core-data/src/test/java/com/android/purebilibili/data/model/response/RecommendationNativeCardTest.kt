package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RecommendationNativeCardTest {
    @Test
    fun mobileRecommendationPreservesPublishTimeWithoutInventingMissingDates() {
        val published = json.decodeFromString<MobileFeedItem>(
            """{"goto":"av","param":"123","pubdate":1793188800}"""
        ).toVideoItem()
        assertEquals(1793188800L, published.pubdate)
        assertEquals(0L, json.decodeFromString<MobileFeedItem>("""{"goto":"av","param":"123"}""").toVideoItem().pubdate)
        assertEquals(0L, MobileFeedItem(pubdate = -1L).toVideoItem().pubdate)
    }

    @Test
    fun endpointStatusFlagsSurviveBothRecommendationConversions() {
        val payload = """{"is_steins":1,"is_lesson_video":true,"is_charging_arc":1,"is_union_video":1}"""
        val expected = listOf("充电专属", "课堂", "合作", "互动")
        assertEquals(expected, json.decodeFromString<RecommendItem>(payload).toVideoItem().statusLabels)
        assertEquals(expected, json.decodeFromString<MobileFeedItem>(payload).toVideoItem().statusLabels)
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun cardGotoRoutesBangumiWithoutLeakingEpisodeAsAv() {
        val raw = json.decodeFromString<MobileFeedItem>(
            """{"goto":"av","card_goto":"bangumi","param":"123","cover_right_text":"更新至12话","desc_button":{"text":"番剧"},"player_args":{"aid":999,"cid":888}}""",
        )
        assertTrue(raw.isSupportedRecommendation)
        val card = raw.toVideoItem()
        assertEquals("bangumi:123", card.bvid)
        assertEquals(123L, card.episodeId)
        assertEquals(0L, card.aid)
        assertEquals(0L, card.cid)
        assertEquals("番剧", card.owner.name)
        assertTrue(card.statusLabels.containsAll(listOf("番剧", "更新至12话")))
        assertEquals(card, json.decodeFromString<VideoItem>(json.encodeToString(card)))
    }

    @Test
    fun pictureHasDistinctIdentityLabelAndNoPlaybackMetadata() {
        val card = json.decodeFromString<MobileFeedItem>(
            """{"card_goto":"picture","param":"123","duration":60}""",
        ).toVideoItem()
        assertEquals("dynamic:123", card.bvid)
        assertEquals("123", card.dynamicId)
        assertEquals(0, card.duration)
        assertEquals(listOf("动态"), card.statusLabels)
        assertNotEquals("bangumi:123", card.bvid)
    }

    @Test
    fun nativeUrisResolveWhenParamIsMissing() {
        assertEquals(123L, recommendationEpisodeId("", "https://www.bilibili.com/bangumi/play/ep123"))
        assertEquals(123L, recommendationEpisodeId("", "bilibili://bangumi/play/123"))
        assertEquals("456", recommendationDynamicId("", "https://t.bilibili.com/456"))
        assertEquals("456", recommendationDynamicId("", "bilibili://following/detail/456"))
        assertEquals(0L, recommendationEpisodeId("bad", ""))
        assertEquals("", recommendationDynamicId("0", ""))
    }

    @Test
    fun adsAndUnsupportedCardsRemainFiltered() {
        listOf(
            """{"card_goto":"ad_av","param":"123"}""",
            """{"goto":"av","param":"123","ad_info":{}}""",
            """{"card_goto":"live","param":"123"}""",
        ).forEach { assertFalse(json.decodeFromString<MobileFeedItem>(it).isSupportedRecommendation) }
        assertEquals("", MobileFeedItem(cardGoto = "bangumi", param = "bad").toVideoItem().bvid)
    }

    @Test
    fun webNativeCardsPreserveTypeAndOrdinaryVideosKeepBvid() {
        val episode = json.decodeFromString<RecommendItem>(
            """{"goto":"bangumi","uri":"https://www.bilibili.com/bangumi/play/ep123"}""",
        ).toVideoItem()
        assertEquals(123L, episode.episodeId)
        val dynamic = json.decodeFromString<RecommendItem>(
            """{"goto":"picture","uri":"https://t.bilibili.com/456"}""",
        ).toVideoItem()
        assertEquals("456", dynamic.dynamicId)
        assertEquals("BV1test", RecommendItem(bvid = "BV1test").toVideoItem().bvid)
    }
}
