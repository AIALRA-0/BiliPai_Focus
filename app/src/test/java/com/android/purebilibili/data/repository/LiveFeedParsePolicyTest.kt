package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.LiveFeedCard
import com.android.purebilibili.data.model.response.LiveFeedCardData
import com.android.purebilibili.data.model.response.LiveFeedIndexData
import com.android.purebilibili.data.model.response.LiveFeedModuleBlock
import com.android.purebilibili.data.model.response.LiveFeedRoomCard
import com.android.purebilibili.data.model.response.LiveRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class LiveFeedParsePolicyTest {
    @Test
    fun missingFollowCountRemainsUnknownWhileExplicitZeroIsKept() {
        assertEquals(null, parseLiveFeedHomeSnapshot(LiveFeedIndexData()).totalCount)
        val data = LiveFeedIndexData(cardList = listOf(LiveFeedCard(
            cardType = "my_idol_v1", cardData = LiveFeedCardData(myIdolV1 = LiveFeedModuleBlock(
                extraInfo = com.android.purebilibili.data.model.response.LiveFeedExtraInfo(totalCount = 0),
            )),
        )))
        assertEquals(0, parseLiveFeedHomeSnapshot(data).totalCount)
    }

    @Test
    fun recommendationFeedbackReasonsSurviveRoomMapping() {
        val card = Json.decodeFromString<LiveFeedRoomCard>(
            """{"roomid":42,"feedback":[{"title":"不感兴趣","reasons":[{"id":12,"name":"主播","id_type":"uid","reason_id":7}]}]}"""
        )
        assertEquals("uid", card.toLiveRoom().feedback.single().reasons.single().idType)
        assertEquals(12L, card.toLiveRoom().feedback.single().reasons.single().id)
    }


    @Test
    fun feedRoomCardMapsToLiveRoomWithSystemCover() {
        val room = LiveFeedRoomCard(
            roomid = 42,
            title = "Hello",
            uname = "UP",
            cover = "https://cover",
            systemCover = "https://frame",
            areaName = "娱乐",
            online = 123,
        ).toLiveRoom()

        assertEquals(42L, room.roomid)
        assertEquals("https://cover", room.cover)
        assertEquals("https://frame", room.systemCover)
        assertEquals("娱乐", room.areaName)
        assertEquals("https://frame", room.displayCover(preferFirstFrame = true))
        assertEquals("https://cover", room.displayCover(preferFirstFrame = false))
    }

    @Test
    fun displayCoverPrefersFirstFrameWhenRequested() {
        val room = LiveRoom(
            roomid = 1,
            cover = "cover",
            systemCover = "frame",
            keyframe = "key",
        )
        assertEquals("frame", room.displayCover(preferFirstFrame = true))
        assertEquals("cover", room.displayCover(preferFirstFrame = false))
    }

    @Test
    fun areaEntranceAcceptsLegacyAreaIdFieldNames() {
        val entry = Json.decodeFromString<LiveFeedRoomCard>(
            """{"title":"网游","area_id":2,"parent_area_id":0}"""
        )

        assertTrue(isUsableLiveFeedAreaEntry(entry))
        assertEquals(2, entry.areaV2Id)
        assertEquals(0, entry.areaV2ParentId)
    }

    @Test
    fun parseFeedDropsBannerAndAdsAndKeepsUsableRooms() {
        val snapshot = parseLiveFeedHomeSnapshot(
            LiveFeedIndexData(
                cardList = listOf(
                    LiveFeedCard(cardType = "banner_v2"),
                    LiveFeedCard(
                        cardType = "small_card_v1",
                        cardData = LiveFeedCardData(
                            smallCardV1 = LiveFeedRoomCard(
                                roomid = 1,
                                title = "ad room",
                                cover = "c",
                                isAd = true,
                            )
                        )
                    ),
                    LiveFeedCard(
                        cardType = "small_card_v1",
                        cardData = LiveFeedCardData(
                            smallCardV1 = LiveFeedRoomCard(
                                roomid = 2,
                                title = "good",
                                cover = "cover",
                            )
                        )
                    ),
                    LiveFeedCard(
                        cardType = "my_idol_v1",
                        cardData = LiveFeedCardData(
                            myIdolV1 = LiveFeedModuleBlock(
                                list = listOf(
                                    LiveFeedRoomCard(roomid = 7, title = "followed", cover = "f"),
                                    LiveFeedRoomCard(roomid = 0, title = "bad"),
                                )
                            )
                        )
                    ),
                    LiveFeedCard(
                        cardType = "area_entrance_v3",
                        cardData = LiveFeedCardData(
                            areaEntranceV3 = LiveFeedModuleBlock(
                                list = listOf(
                                    LiveFeedRoomCard(
                                        title = "游戏",
                                        areaV2Id = 1,
                                        areaV2ParentId = 2,
                                    ),
                                    LiveFeedRoomCard(title = ""),
                                )
                            )
                        )
                    ),
                ),
                hasMore = 1,
            )
        )

        assertEquals(1, snapshot.rooms.size)
        assertEquals(2L, snapshot.rooms.first().roomid)
        assertEquals(1, snapshot.followRooms.size)
        assertEquals(7L, snapshot.followRooms.first().roomid)
        assertEquals(1, snapshot.areaEntries.size)
        assertEquals("游戏", snapshot.areaEntries.first().title)
        assertTrue(snapshot.hasMore)
    }

    @Test
    fun usableRoomFilterRejectsEmptyShellCards() {
        assertFalse(
            isUsableLiveFeedRoom(
                LiveFeedRoomCard(roomid = 3, title = "", cover = "", systemCover = "")
            )
        )
        assertTrue(
            isUsableLiveFeedRoom(
                LiveFeedRoomCard(roomid = 3, title = "ok", cover = "c")
            )
        )
    }
    @Test
    fun followAvatarsDoNotNeedBroadcastTitleOrCover() {
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<LiveFeedIndexData>("""
          {"card_list":[{"card_type":"my_idol_v1","card_data":{"my_idol_v1":{
            "extra_info":{"total_count":18},"list":[
              {"roomid":123,"uid":7,"uname":"主播","face":"avatar"},
              {"roomid":123,"uid":7,"uname":"主播","face":"avatar"},
              {"roomid":0,"uname":"无效房间"}
            ]}}}]}
        """.trimIndent())
        val snapshot = parseLiveFeedHomeSnapshot(data)
        assertTrue(snapshot.hasFollowModule)
        assertEquals(18, snapshot.totalCount)
        assertEquals(1, snapshot.followRooms.size)
        assertEquals("avatar", snapshot.followRooms.single().face)
        assertEquals(123L, snapshot.followRooms.single().roomid)
        assertFalse(needsLiveFollowSupplement(snapshot))
    }

    @Test
    fun absentUnknownEmptyAndExplicitZeroModulesHaveDifferentSupplementRules() {
        val missing = parseLiveFeedHomeSnapshot(LiveFeedIndexData())
        assertFalse(missing.hasFollowModule)
        assertTrue(needsLiveFollowSupplement(missing))
        val empty = parseLiveFeedHomeSnapshot(LiveFeedIndexData(cardList = listOf(
            LiveFeedCard(cardType = "my_idol_v1", cardData = LiveFeedCardData(myIdolV1 = LiveFeedModuleBlock()))
        )))
        assertTrue(empty.hasFollowModule)
        assertEquals(null, empty.totalCount)
        assertTrue(needsLiveFollowSupplement(empty))
        assertFalse(needsLiveFollowSupplement(empty.copy(totalCount = 0)))
        assertTrue(needsLiveFollowSupplement(empty.copy(totalCount = 18)))
    }

    @Test
    fun supplementalFollowsPreserveRecommendationAndUnknownCount() {
        val base = com.android.purebilibili.data.model.response.LiveFeedHomeSnapshot(
            rooms = listOf(LiveRoom(roomid = 1)), hasMore = true,
        )
        val follow = LiveRoom(roomid = 2, uname = "主播", face = "avatar")
        val merged = supplementLiveFollowSnapshot(base, listOf(follow, follow), null)
        assertTrue(merged.hasFollowModule)
        assertEquals(base.rooms, merged.rooms)
        assertTrue(merged.hasMore)
        assertEquals(1, merged.followRooms.size)
        assertEquals(null, merged.totalCount)
        assertEquals(0, supplementLiveFollowSnapshot(base, emptyList(), 0).totalCount)
    }

}
