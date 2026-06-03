package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfficialVerifyBadgePolicyTest {

    @Test
    fun `official verify policy maps personal and organization badges`() {
        val personal = resolveOfficialVerifyBadge(type = 0, desc = "知名 UP 主")
        val organization = resolveOfficialVerifyBadge(type = 1, desc = "支付宝官方账号")

        assertEquals(OfficialVerifyBadgeTone.PERSONAL, personal?.tone)
        assertEquals("知名 UP 主", personal?.text)
        assertEquals(OfficialVerifyBadgeTone.ORGANIZATION, organization?.tone)
        assertEquals("支付宝官方账号", organization?.text)
    }

    @Test
    fun `official verify policy prefers title over desc and falls back by type`() {
        assertEquals(
            "认证标题",
            resolveOfficialVerifyBadge(type = 0, title = "认证标题", desc = "认证备注")?.text
        )
        assertEquals("个人认证", resolveOfficialVerifyBadge(type = 0)?.text)
        assertEquals("机构认证", resolveOfficialVerifyBadge(type = 1)?.text)
    }

    @Test
    fun `official verify policy emits compact labels without losing semantic text`() {
        val compact = resolveOfficialVerifyBadge(
            type = 1,
            title = "支付宝官方账号",
            compact = true
        )

        assertEquals("机构", compact?.text)
        assertEquals("支付宝官方账号", compact?.contentDescription)
    }

    @Test
    fun `official verify policy hides unverified and unknown types`() {
        assertNull(resolveOfficialVerifyBadge(type = -1, desc = ""))
        assertNull(resolveOfficialVerifyBadge(type = 99, desc = "未知认证"))
    }

    @Test
    fun `official verify policy maps staff role to badge tone when type means verified`() {
        val personal = resolveOfficialVerifyBadgeFromRole(
            type = 0,
            role = 1,
            title = "知名 UP 主"
        )
        val organization = resolveOfficialVerifyBadgeFromRole(
            type = 0,
            role = 3,
            title = "企业认证"
        )

        assertEquals(OfficialVerifyBadgeTone.PERSONAL, personal?.tone)
        assertEquals(OfficialVerifyBadgeTone.ORGANIZATION, organization?.tone)
    }
}
