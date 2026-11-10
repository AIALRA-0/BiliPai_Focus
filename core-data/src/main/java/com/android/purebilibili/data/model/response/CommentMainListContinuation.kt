package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
enum class CommentMainListRoute { GRPC, REST_MAIN, REST_LEGACY }

/** 同一轮主评论分页必须沿用同一接口，游标不能与本地页码互换。 */
@Serializable
data class CommentMainListContinuation(
    val route: CommentMainListRoute,
    val next: Long = 0L,
    val offset: String? = null,
)
