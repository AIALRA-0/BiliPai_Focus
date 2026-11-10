package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class FollowGroupOrderResponse(
    val code: Int = -1,
    val message: String = "",
    val data: List<RelationTagItem>? = null,
)

@Serializable
data class FollowGroupOrderSaveResponse(val code: Int = -1, val message: String = "")
