package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateReleaseNotesPolicyTest {

    @Test
    fun `resolveUpdateReleaseNotesText should return placeholder when notes blank`() {
        assertEquals("暂无更新说明", resolveUpdateReleaseNotesText("   "))
    }

    @Test
    fun `resolveUpdateReleaseNotesText should keep long notes without truncation`() {
        val longNotes = buildString {
            append("更新内容：")
            repeat(80) { append("功能优化、修复问题。") }
        }

        val resolved = resolveUpdateReleaseNotesText(longNotes)

        assertEquals(longNotes, resolved)
        assertTrue(resolved.length > 240)
    }

    @Test
    fun `parseUpdateReleaseNotes should preserve headings lists dividers and paragraphs`() {
        val blocks = parseUpdateReleaseNotes(
            """
            # 7.4.0
            - 修复播放器
            1. 优化更新下载
            ---
            其他说明
            """.trimIndent()
        )

        assertEquals(AppUpdateReleaseNotesBlock.Heading("7.4.0", 1), blocks[0])
        assertEquals(AppUpdateReleaseNotesBlock.Bullet("修复播放器", false), blocks[1])
        assertEquals(AppUpdateReleaseNotesBlock.Bullet("优化更新下载", true), blocks[2])
        assertEquals(AppUpdateReleaseNotesBlock.Divider, blocks[3])
        assertEquals(AppUpdateReleaseNotesBlock.Paragraph("其他说明"), blocks[4])
    }
}
