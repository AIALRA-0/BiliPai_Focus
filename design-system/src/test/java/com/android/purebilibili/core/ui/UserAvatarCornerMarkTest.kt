package com.android.purebilibili.core.ui

import com.android.purebilibili.core.ui.components.resolveContentSizedTabScrollOffsetPx
import kotlin.test.Test
import kotlin.test.assertEquals

class UserAvatarCornerMarkTest {
    @Test
    fun `live hides the corner mark and official wins over vip`() {
        assertEquals(
            UserAvatarCornerMark.None,
            resolveUserAvatarCornerMark(officialType = -1, vipStatus = 1, isLive = true),
        )
        assertEquals(
            UserAvatarCornerMark.Vip,
            resolveUserAvatarCornerMark(officialType = -1, vipStatus = 1),
        )
        assertEquals(
            UserAvatarCornerMark.None,
            resolveUserAvatarCornerMark(officialType = null, vipStatus = 0),
        )
        assertEquals(
            UserAvatarCornerMark.Personal,
            resolveUserAvatarCornerMark(officialType = 0, vipStatus = 1),
        )
        assertEquals(
            UserAvatarCornerMark.Organization,
            resolveUserAvatarCornerMark(officialType = 1, vipStatus = 1),
        )
    }

    @Test
    fun `content sized underline scrolls the selected tab toward the center`() {
        assertEquals(0, resolveContentSizedTabScrollOffsetPx(0, 60, 400, 800))
        assertEquals(170, resolveContentSizedTabScrollOffsetPx(340, 60, 400, 800))
        assertEquals(800, resolveContentSizedTabScrollOffsetPx(2000, 60, 400, 800))
    }
}
