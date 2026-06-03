package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockedListScreenPolicyTest {

    @Test
    fun blockedUpMetaLine_includesLevelAndAccountState() {
        assertEquals(
            "UID 42 · LV5 · 疑似已注销 · 年度大会员 · 认证用户 · 粉丝 1000 · 投稿 12",
            buildBlockedUpMetaLine(
                mid = 42L,
                level = 5,
                vipLabel = "年度大会员",
                officialTitle = "认证用户",
                follower = 1000L,
                archiveCount = 12,
                isDeleted = true
            )
        )
    }

    @Test
    fun blockedUpMetaLine_doesNotShowUnknownLevelWhenProfileIsNotLoaded() {
        assertEquals(
            "UID 42",
            buildBlockedUpMetaLine(
                mid = 42L,
                level = null,
                vipLabel = "",
                officialTitle = "",
                follower = null,
                archiveCount = null,
                isDeleted = false
            )
        )
    }
}
