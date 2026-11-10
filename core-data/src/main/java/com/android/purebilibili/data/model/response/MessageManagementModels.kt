package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageLikeDetailResponse(val code: Int = 0, val message: String = "", val data: MessageLikeDetailData? = null)
@Serializable
data class MessageLikeDetailData(
    val page: MessageLikeDetailPage? = null,
    val card: MessageLikeDetailCard? = null,
    val items: List<MessageLikeDetailItem> = emptyList(),
)
@Serializable
data class MessageLikeDetailPage(@SerialName("is_end") val isEnd: Boolean = true)
@Serializable
data class MessageLikeDetailCard(val business: String = "", val title: String = "")
@Serializable
data class MessageLikeDetailItem(val user: MessageLikeDetailUser? = null, @SerialName("like_time") @Serializable(with = FlexibleLongSerializer::class) val likeTime: Long = 0)
@Serializable
data class MessageLikeDetailUser(@Serializable(with = FlexibleLongSerializer::class) val mid: Long = 0, val nickname: String = "", val avatar: String = "")
