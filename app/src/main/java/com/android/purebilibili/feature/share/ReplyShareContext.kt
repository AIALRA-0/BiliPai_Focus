package com.android.purebilibili.feature.share

import androidx.compose.runtime.compositionLocalOf

/** 评论所在内容的身份；列表与楼中楼沿用同一份来源数据。 */
internal data class ReplyShareContext(
    val url: String,
    val title: String = "",
    val cover: String = "",
    val page: Int = 1,
)
internal val LocalReplyShareContext = compositionLocalOf<ReplyShareContext?> { null }
