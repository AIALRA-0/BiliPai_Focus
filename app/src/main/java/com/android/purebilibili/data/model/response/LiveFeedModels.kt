package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

/**
 * App 端直播首页 feed（对齐 BiliPai `xlive/app-interface/v2/index/feed`）。
 */
@Serializable
data class LiveFeedIndexResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveFeedIndexData? = null,
)

@Serializable
data class LiveFeedIndexData(
    @SerialName("card_list") val cardList: List<LiveFeedCard>? = null,
    @SerialName("has_more") val hasMore: Int = 0,
)

@Serializable
data class LiveFeedCard(
    @SerialName("card_type") val cardType: String = "",
    @SerialName("card_data") val cardData: LiveFeedCardData? = null,
)

@Serializable
data class LiveFeedCardData(
    @SerialName("small_card_v1") val smallCardV1: LiveFeedRoomCard? = null,
    @SerialName("my_idol_v1") val myIdolV1: LiveFeedModuleBlock? = null,
    @SerialName("area_entrance_v3") val areaEntranceV3: LiveFeedModuleBlock? = null,
)

@Serializable
data class LiveFeedModuleBlock(
    val list: List<LiveFeedRoomCard>? = null,
    @SerialName("extra_info") val extraInfo: LiveFeedExtraInfo? = null,
)

@Serializable
data class LiveFeedExtraInfo(
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LiveFeedRoomCard(
    @JsonNames("roomid", "room_id", "id")
    val roomid: Long = 0,
    val uid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    @SerialName("system_cover") val systemCover: String = "",
    val title: String = "",
    @JsonNames("area_name", "area_v2_name")
    val areaName: String = "",
    @JsonNames("area_v2_id", "area_id") val areaV2Id: Int = 0,
    @JsonNames("area_v2_parent_id", "parent_area_id") val areaV2ParentId: Int = 0,
    @SerialName("watched_show") val watchedShow: WatchedShow? = null,
    val online: Int = 0,
    @SerialName("is_ad") val isAd: Boolean = false,
) {
    fun resolvedRoomId(): Long = roomid

    fun toLiveRoom(): LiveRoom = LiveRoom(
        roomid = resolvedRoomId(),
        uid = uid,
        title = title,
        uname = uname,
        face = face,
        cover = cover,
        systemCover = systemCover,
        online = watchedShow?.viewerCount()?.takeIf { it > 0 } ?: online,
        watchedShow = watchedShow,
        areaName = areaName,
        keyframe = systemCover.ifBlank { cover },
    )
}

@Serializable
data class LiveAppSecondListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveAppSecondListData? = null,
)

@Serializable
data class LiveAppSecondListData(
    val count: Int = 0,
    val list: List<LiveFeedRoomCard>? = null,
    @SerialName("new_tags") val newTags: List<LiveSecondSortTag>? = null,
    @SerialName("has_more") val hasMore: Int = 0,
)

@Serializable
data class LiveSecondSortTag(
    val name: String = "",
    @SerialName("sort_type") val sortType: String = "",
)

/**
 * Domain snapshot for Live home after parsing feed/second list.
 */
data class LiveFeedHomeSnapshot(
    val rooms: List<LiveRoom> = emptyList(),
    val followRooms: List<LiveRoom> = emptyList(),
    val areaEntries: List<LiveFeedAreaEntry> = emptyList(),
    val sortTags: List<LiveSecondSortTag> = emptyList(),
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
)

data class LiveFeedAreaEntry(
    val title: String,
    val areaId: Int,
    val parentAreaId: Int,
)
