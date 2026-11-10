package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * B站官方 PBP (Play-by-play / 高能进度条趋势) 数据模型
 *
 * 接口: GET https://bvc.bilivideo.com/pbp/data?bvid={bvid}&cid={cid}
 */
@Serializable
data class PbpResponse(
    val code: Int = 0,
    val message: String = "",
    @SerialName("step_sec") val stepSec: Int = 0,
    val events: PbpEvents? = null,
    val data: PbpData? = null
)

@Serializable
data class PbpData(
    @SerialName("step_sec") val stepSec: Int = 0,
    val events: PbpEvents? = null
)

@Serializable
data class PbpEvents(
    @SerialName("default") val defaultCurve: List<Float> = emptyList()
)
