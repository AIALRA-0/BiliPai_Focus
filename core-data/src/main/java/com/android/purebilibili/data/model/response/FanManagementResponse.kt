package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FanRemovalResponse(val code: Int = -1, val message: String = "")

@Serializable
data class FanRelationshipResponse(
    val code: Int = -1,
    val message: String = "",
    val data: FanRelationshipData? = null,
)

@Serializable
data class FanRelationshipData(
    @SerialName("be_relation") val reverseRelation: RelationData? = null,
)
