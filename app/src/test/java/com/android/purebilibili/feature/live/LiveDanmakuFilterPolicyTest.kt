package com.android.purebilibili.feature.live

import com.android.purebilibili.data.repository.LiveShieldInfo
import com.android.purebilibili.data.repository.LiveShieldKeyword
import com.android.purebilibili.data.repository.LiveShieldUser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDanmakuFilterPolicyTest {
    @Test
    fun `keyword and user rules independently block matching messages`() {
        val filter = LiveDanmakuFilterPolicy(
            LiveShieldInfo(
                keywords = listOf(LiveShieldKeyword(keyword = "广告")),
                users = listOf(LiveShieldUser(uid = 42))
            )
        )
        assertTrue(filter.blocks(1, "广告链接"))
        assertTrue(filter.blocks(42, "正常内容"))
        assertFalse(filter.blocks(43, "正常内容"))
    }

    @Test
    fun `blank keywords and invalid user IDs cannot block every anonymous message`() {
        val filter = LiveDanmakuFilterPolicy(
            LiveShieldInfo(
                keywords = listOf(LiveShieldKeyword(keyword = ""), LiveShieldKeyword(keyword = " ")),
                users = listOf(LiveShieldUser(uid = 0), LiveShieldUser(uid = -1))
            )
        )
        assertFalse(filter.blocks(0, "正常内容"))
        assertFalse(filter.blocks(-1, "正常内容"))
    }

    @Test
    fun `removing rules allows later matching messages without applying unknown level semantics`() {
        val old = LiveDanmakuFilterPolicy(LiveShieldInfo(keywords = listOf(LiveShieldKeyword(keyword = "广告"))))
        assertTrue(old.blocks(42, "广告"))
        val refreshed = LiveDanmakuFilterPolicy(LiveShieldInfo(level = 99, medal = 99, verify = 1, rank = 1, phone = 1))
        assertFalse(refreshed.blocks(42, "广告"))
    }
}
