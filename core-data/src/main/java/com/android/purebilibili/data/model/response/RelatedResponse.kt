package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class RelatedResponse(
    val data: List<RelatedVideo>? = null,
    val code: Int = 0,
    val message: String = "",
)

@Serializable
data class RelatedVideo(
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
    val duration: Int = 0, // 视频时长(秒)
    val pubdate: Long = 0,
    /** null means the lightweight source did not expose trustworthy dimensions. */
    val isVertical: Boolean? = null,
)

/** 相关推荐条目与通用 VideoItem 的映射，供目录/详情列表消费。 */
fun RelatedVideo.toVideoItem(): VideoItem = VideoItem(
    id = aid, bvid = bvid, aid = aid, cid = cid, title = title, pic = pic,
    owner = owner, stat = stat, duration = duration, pubdate = pubdate,
    isVertical = isVertical == true,
)
