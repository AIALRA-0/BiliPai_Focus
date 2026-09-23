package com.android.purebilibili.data.repository

data class LivePagedResult<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val nextPage: Int,
    val totalCount: Int = 0
)

data class LiveDanmakuSendRequest(
    val roomId: Long,
    val message: String,
    val color: Int = 16777215,
    val fontSize: Int = 25,
    val mode: Int = 1,
    val bubble: Int = 0,
    val roomType: Int = 0,
    val jumpFrom: Int = 0,
    val replyMid: Long = 0,
    val replyAttr: Int = 0,
    val replyUname: String = "",
    val replayDmid: String = "",
    val statistics: String = """{"appId":100,"platform":5}""",
    val dmType: Int? = null,
    val emoticonOptions: String? = null
)

data class LiveDanmakuReportRequest(
    val roomId: Long,
    val uid: Long,
    val uname: String,
    val message: String,
    val dmid: String,
    val reportTime: Long,
    val sign: String,
    val reason: LiveReportReason
)

data class LiveSuperChatReportRequest(
    val roomId: Long,
    val uid: Long,
    val uname: String,
    val message: String,
    val messageId: Long,
    val token: String,
    val reportTime: Long,
    val reason: LiveReportReason
)

data class LiveReportReason(
    val id: Int,
    val label: String,
    val apiReason: String
)

data class LiveShieldInfo(
    val level: Int = 0,
    val medal: Int = 0,
    val verify: Int = 0,
    // [新增] 非正式会员 / 未绑定手机 屏蔽规则（0=关，>0=开）
    val rank: Int = 0,
    val phone: Int = 0,
    val keywords: List<LiveShieldKeyword> = emptyList(),
    val users: List<LiveShieldUser> = emptyList()
)

data class LiveShieldKeyword(
    val id: Long = 0,
    val keyword: String
)

data class LiveShieldUser(
    val uid: Long,
    val uname: String = "",
    val face: String = "",
    val id: Long = 0
)

data class LiveEmoticonPackage(
    val id: Int,
    val name: String,
    val items: List<LiveEmoticonItem>
)

data class LiveEmoticonItem(
    val emoji: String,
    val url: String,
    val description: String = "",
    // 表情唯一标识（emoticon_unique），用于发送表情弹幕
    val emoticonUnique: String = "",
    val emoticonOptions: String? = null
)

data class LiveVoteOption(val id: Int = 0, val description: String = "", val percent: Float = 0f)

data class LiveVoteInfo(
    val status: Int = 0,
    val question: String = "",
    val options: List<LiveVoteOption> = emptyList(),
    val durationMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val resultText: String = "",
    val endTimeText: String = "",
    val interactionId: Long = 0L
) {
    val isActive: Boolean get() = status == 4
}

data class LiveVoteSnapshot(
    val current: LiveVoteInfo? = null,
    val history: List<LiveVoteInfo> = emptyList()
)

val DefaultLiveReportReasons = listOf(
    LiveReportReason(id = 1, label = "违法违禁", apiReason = "违法违禁"),
    LiveReportReason(id = 2, label = "色情低俗", apiReason = "色情低俗"),
    LiveReportReason(id = 3, label = "赌博诈骗", apiReason = "赌博诈骗"),
    LiveReportReason(id = 4, label = "人身攻击", apiReason = "人身攻击"),
    LiveReportReason(id = 5, label = "垃圾广告", apiReason = "垃圾广告"),
    LiveReportReason(id = 6, label = "其他", apiReason = "其他")
)
