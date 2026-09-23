package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class DynamicVoteInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DynamicVoteInfoPayload? = null
)

@Serializable
data class DynamicVoteInfoPayload(
    val vote_info: DynamicVoteInfo? = null,
    val my_votes: List<Int> = emptyList()
)

@Serializable
data class DynamicVoteInfo(
    val vote_id: Long = 0,
    val title: String = "",
    val desc: String = "",
    val end_time: Long = 0,
    val join_num: Int = 0,
    val choice_cnt: Int = 1,
    val my_votes: List<Int> = emptyList(),
    val options: List<DynamicVoteOption> = emptyList()
)

@Serializable
data class DynamicVoteOption(
    val opt_idx: Int = 0,
    val opt_desc: String = "",
    val cnt: Int = 0,
    val img_url: String = ""
)

@Serializable
data class DynamicDoVoteRequest(
    val vote_id: Long,
    val votes: List<Int>,
    val voter_uid: Long,
    val status: Int = 0,
    val op_bit: Int = 0,
    val dynamic_id: Long = 0,
    val csrf: String = "",
    val csrf_token: String = ""
)

internal fun DynamicVoteInfoPayload.toResolvedVoteInfo(): DynamicVoteInfo? {
    val info = vote_info ?: return null
    if (info.vote_id <= 0L) return null
    return if (info.my_votes.isNotEmpty() || my_votes.isEmpty()) {
        info
    } else {
        info.copy(my_votes = my_votes)
    }
}