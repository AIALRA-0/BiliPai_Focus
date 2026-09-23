package com.android.purebilibili.feature.livesite

import kotlinx.serialization.Serializable

interface LiveSite {
    val id: String
    val displayName: String

    suspend fun getCategories(): Result<List<LiveSiteCategory>>
    suspend fun getRecommendRooms(page: Int): Result<LiveSiteRoomPage>
    suspend fun getCategoryRooms(areaId: String, page: Int): Result<LiveSiteRoomPage>
    suspend fun searchRooms(keyword: String, page: Int): Result<LiveSiteRoomPage>
    suspend fun getRoomDetail(roomId: String): Result<LiveSiteRoomDetail>
    suspend fun getPlayUrls(detail: LiveSiteRoomDetail, qualityId: String?): Result<List<LiveSiteStream>>
}

@Serializable
data class LiveSiteCategory(
    val id: String,
    val name: String,
    val areas: List<LiveSiteArea> = emptyList()
)

@Serializable
data class LiveSiteArea(
    val id: String,
    val name: String,
    val pic: String = ""
)

@Serializable
data class LiveSiteRoom(
    val roomId: String,
    val title: String,
    val nick: String,
    val cover: String,
    val avatar: String = "",
    val area: String = "",
    val watching: String = "",
    val live: Boolean = true
)

@Serializable
data class LiveSiteRoomPage(
    val items: List<LiveSiteRoom>,
    val hasMore: Boolean
)

@Serializable
data class LiveSiteRoomDetail(
    val roomId: String,
    val title: String,
    val nick: String,
    val cover: String,
    val avatar: String = "",
    val area: String = "",
    val watching: String = "",
    val live: Boolean,
    val notice: String = "",
    val introduction: String = "",
    val qualities: List<LiveSiteQuality> = emptyList(),
    val playData: String = ""
)

@Serializable
data class LiveSiteQuality(
    val id: String,
    val name: String
)

@Serializable
data class LiveSiteStream(
    val url: String,
    val title: String = "线路",
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap()
)
