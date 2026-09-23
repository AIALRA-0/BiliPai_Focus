package com.android.purebilibili.feature.video.ui.section

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiSummaryTimestampLayoutPolicyTest {

    @Test
    fun `format pads mm ss`() {
        assertEquals("00:01", formatAiSummaryTimestamp(1))
        assertEquals("00:09", formatAiSummaryTimestamp(9))
        assertEquals("00:19", formatAiSummaryTimestamp(19))
        assertEquals("01:28", formatAiSummaryTimestamp(88))
        assertEquals("00:00", formatAiSummaryTimestamp(-3))
    }

    @Test
    fun `outline timestamp chip fills fixed column for vertical alignment`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/AiSummarySection.kt"
        )
        assertTrue(source.contains("OutlineTimestampColumnWidth"))
        assertTrue(source.contains("OutlineBulletSlotWidth"))
        // 满宽芯片 + 居中，避免比例数字导致时钟图标参差
        assertTrue(source.contains(".fillMaxWidth()"))
        assertTrue(source.contains("Arrangement.Center"))
        assertTrue(source.contains("fontFeatureSettings = \"tnum\""))
        assertTrue(source.contains("formatAiSummaryTimestamp"))
    }

    private fun loadSource(path: String): String {
        val normalized = path.removePrefix("app/")
        val file = listOf(File(path), File(normalized)).firstOrNull { it.exists() }
            ?: error("Cannot locate $path")
        return file.readText()
    }
}
