package com.android.purebilibili.feature.following

import com.android.purebilibili.core.ui.OfficialVerifyBadgeSpec
import com.android.purebilibili.core.ui.OfficialVerifyBadgeTone
import com.android.purebilibili.core.ui.resolveOfficialVerifyBadge
import com.android.purebilibili.data.model.response.OfficialVerify
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FollowingSinceFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

typealias FollowingOfficialVerifyBadgeTone = OfficialVerifyBadgeTone
typealias FollowingOfficialVerifyBadge = OfficialVerifyBadgeSpec

internal fun resolveFollowingOfficialVerifyBadge(
    officialVerify: OfficialVerify
): FollowingOfficialVerifyBadge? {
    return resolveOfficialVerifyBadge(
        type = officialVerify.type,
        desc = officialVerify.desc
    )
}

internal fun formatFollowingSinceLabel(
    mtimeSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (mtimeSeconds <= 0L) return ""
    val date = Instant.ofEpochSecond(mtimeSeconds).atZone(zoneId).toLocalDate()
    return "关注于 ${FollowingSinceFormatter.format(date)}"
}
