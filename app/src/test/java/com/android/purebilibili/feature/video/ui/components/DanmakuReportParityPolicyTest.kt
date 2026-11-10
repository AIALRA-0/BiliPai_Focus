package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.*
import org.junit.Test

class DanmakuReportParityPolicyTest {
    @Test fun reportReasonCodesMatchServerAndOtherRequiresExplanation() {
        assertEquals(listOf(1,2,3,4,5,6,7,8,9,10,12,13,11), DanmakuReportReasons.map { it.second })
        assertFalse(canSubmitDanmakuReport(null, ""))
        assertFalse(canSubmitDanmakuReport(99, "details"))
        assertTrue(canSubmitDanmakuReport(1, ""))
        assertFalse(canSubmitDanmakuReport(11, " \n "))
        assertTrue(canSubmitDanmakuReport(11, "真实原因"))
        assertTrue(canSubmitDanmakuReport(2, ""))
    }

    @Test fun timestampMustBeInsideActualDuration() {
        assertNull(resolveValidDanmakuTimestampJumpMs("00:00", 120000))
        assertNull(resolveValidDanmakuTimestampJumpMs("02:00", 120000))
        assertNull(resolveValidDanmakuTimestampJumpMs("02:01", 120000))
        assertNull(resolveValidDanmakuTimestampJumpMs("01:00", 0))
        assertEquals(60000L, resolveValidDanmakuTimestampJumpMs("看 01:00", 120000))
        assertNull(resolveValidDanmakuTimestampJumpMs("01:99", 120000))
        assertNull(resolveValidDanmakuTimestampJumpMs("100:00", 99999999))
    }

    @Test fun unavailableGradientAndOutOfRgbDraftUseWhite() {
        val colors = danmakuSendColorOptions().map { it.value }
        assertEquals(listOf(0xFFFFFF,0xFE0302,0xFF7204,0xFFAA02,0xFFD302,0xFFFF00,
            0xA0EE00,0x00CD00,0x019899,0x4266BE,0x89D5FF,0xCC0273,0x222222,0x9B9B9B), colors)
        val resolved = resolveDanmakuSendSelectionState(-1, 99, 36, colors, listOf(1,5,4), listOf(18,25))
        assertEquals(DanmakuSendSelectionState(0xFFFFFF,1,25), resolved)
        assertEquals(0x123456, resolveDanmakuSendSelectionState(0x123456,1,18,colors,listOf(1,5,4),listOf(18,25)).color)
    }
}
