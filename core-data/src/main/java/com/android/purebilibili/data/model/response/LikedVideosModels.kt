package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class LikedVideosResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LikedVideosData? = null
)

@Serializable
data class LikedVideosData(
    val count: Int = 0,
    val list: List<LikedVideoData> = emptyList(),
    val item: List<SpaceAggregateArchiveItem> = emptyList(),
)

@Serializable
data class LikedVideoData(
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val duration: Int = 0,
    val pubdate: Long = 0,
    val tid: Int = 0,
    val tname: String = "",
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
    val dimension: Dimension? = null
) {
    fun toVideoItem(): VideoItem {
        return VideoItem(
            id = aid,
            aid = aid,
            bvid = bvid,
            cid = cid,
            title = title,
            pic = pic,
            owner = owner,
            stat = stat,
            tid = tid,
            tname = tname,
            duration = duration,
            pubdate = pubdate,
            isVertical = dimension?.isVertical == true
        )
    }
}
