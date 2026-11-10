package com.android.purebilibili.data.model.response

/** UP 主卡片聚合信息。原定义位于手机端 VideoRepository.kt，两端共用后迁入共享模型。 */
data class CreatorCardStats(
    val followerCount: Int,
    val videoCount: Int,
    val vipStatus: Int = 0,
    val vipType: Int = 0,
    val officialType: Int = -1,
    val pendantImage: String = "",
)
