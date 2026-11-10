package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SpacePrivacyResponse(
    val code: Int = -1,
    val message: String = "",
    val data: SpacePrivacyData? = null,
)

@Serializable
data class SpacePrivacyData(val privacy: JsonObject? = null)

@Serializable
data class SpacePrivacySaveResponse(
    val code: Int = -1,
    val message: String = "",
)
