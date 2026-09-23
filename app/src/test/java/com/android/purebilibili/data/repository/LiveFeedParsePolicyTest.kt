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
}
