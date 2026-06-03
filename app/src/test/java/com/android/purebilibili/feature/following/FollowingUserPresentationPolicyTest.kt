package com.android.purebilibili.feature.following

import com.android.purebilibili.data.model.response.OfficialVerify
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FollowingUserPresentationPolicyTest {

    @Test
    fun `official verify policy maps personal and organization badges`() {
        val personal = resolveFollowingOfficialVerifyBadge(
            OfficialVerify(type = 0, desc = "知名 UP 主")
        )
        val organization = resolveFollowingOfficialVerifyBadge(
            OfficialVerify(type = 1, desc = "支付宝官方账号")
        )

        assertEquals(FollowingOfficialVerifyBadgeTone.PERSONAL, personal?.tone)
        assertEquals("知名 UP 主", personal?.text)
        assertEquals(FollowingOfficialVerifyBadgeTone.ORGANIZATION, organization?.tone)
        assertEquals("支付宝官方账号", organization?.text)
    }

    @Test
    fun `official verify policy hides unknown verify type`() {
        assertNull(resolveFollowingOfficialVerifyBadge(OfficialVerify(type = -1, desc = "")))
        assertNull(resolveFollowingOfficialVerifyBadge(OfficialVerify(type = 2, desc = "未知")))
    }

    @Test
    fun `follow time policy formats seconds with stable date`() {
        val label = formatFollowingSinceLabel(
            mtimeSeconds = 1704067200,
            zoneId = ZoneId.of("Asia/Shanghai")
        )

        assertEquals("关注于 2024-01-01", label)
    }

    @Test
    fun `follow time policy hides missing mtime`() {
        assertEquals("", formatFollowingSinceLabel(0, ZoneId.of("Asia/Shanghai")))
    }
}
