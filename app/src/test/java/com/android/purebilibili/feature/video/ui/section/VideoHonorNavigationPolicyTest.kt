package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoHonorNavigationPolicyTest {
    @Test
    fun weeklyHonorUsesExplicitNumberBeforeUrlOrText() {
        assertEquals("bilibili://popular/weekly?number=133", resolveVideoHonorJumpUrl(
            2, "https://www.bilibili.com/v/popular/weekly?num=99", 133, "第100期每周必看"
        ))
    }

    @Test
    fun weeklyHonorRecoversMissingNumberFromUrlThenText() {
        assertEquals("bilibili://popular/weekly?number=133", resolveVideoHonorJumpUrl(
            2, "https://www.bilibili.com/v/popular/weekly?num=133", 0
        ))
        assertEquals("bilibili://popular/weekly?number=133", resolveVideoHonorJumpUrl(
            2, "", 0, "第 133 期每周必看"
        ))
        assertEquals("bilibili://popular/weekly", resolveVideoHonorJumpUrl(2, "", 0))
    }

    @Test
    fun otherHonorsKeepTheirNativeDestinations() {
        assertEquals("bilibili://popular/all", resolveVideoHonorJumpUrl(1, "", 0))
        assertEquals("bilibili://popular/rank", resolveVideoHonorJumpUrl(3, "", 0))
        assertEquals("bilibili://popular/comprehensive", resolveVideoHonorJumpUrl(4, "", 0))
    }
}
