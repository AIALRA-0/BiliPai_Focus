package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyPageControl
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentSectionNoticeTest {
    @Test
    fun `web selection and server guidance survive json parsing`() {
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<ReplyData>(
            """{"control":{"web_selection":true,"show_text":"仅展示精选评论","bg_text":"暂无精选内容"}}"""
        )
        assertEquals("精选评论 · 仅展示精选评论", data.control?.sectionNotice())
        assertEquals("暂无精选内容", data.control?.emptyMessage())
    }

    @Test
    fun `ordinary comments do not acquire a curated notice`() {
        for (title in listOf("热门评论", "最热评论", "最新评论")) {
            assertEquals("", ReplyPageControl(title = " $title ", showText = title).sectionNotice())
            assertEquals("精选评论", ReplyPageControl(title = title, webSelection = true).sectionNotice())
        }
        assertEquals("", ReplyPageControl().emptyMessage())
    }

    @Test
    fun `filtering ordinary titles preserves special server guidance`() {
        assertEquals("评论经筛选后公开", ReplyPageControl(
            title = "热门评论", showText = "评论经筛选后公开",
        ).sectionNotice())
        assertEquals("精选评论", ReplyPageControl(title = "精选评论").sectionNotice())
    }
}
