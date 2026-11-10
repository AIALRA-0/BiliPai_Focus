package com.android.purebilibili.feature.live

/** 新消息绑定来源房间；历史消息未携带房间时仅回退到提交瞬间的当前房间。 */
internal fun resolveLiveDanmakuReportRoomId(currentRoomId: Long, originRoomId: Long): Long? {
    if (currentRoomId <= 0L) return null
    val roomId = originRoomId.takeIf { it > 0L } ?: currentRoomId
    return roomId.takeIf { it == currentRoomId }
}
