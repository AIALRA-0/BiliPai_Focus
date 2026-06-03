package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicTimePolicyTest {

    @Test
    fun resolveDynamicAuthorTimeText_prefersPubTsOverStalePubTime() {
        val nowMs = 1_700_003_480_000L

        assertEquals(
            "58分钟前",
            resolveDynamicAuthorTimeText(
                pubTime = "刚刚",
                pubTs = 1_700_000_000L,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun resolveDynamicAuthorTimeText_fallsBackToPubTimeWhenTimestampMissing() {
        assertEquals(
            "昨天 22:07",
            resolveDynamicAuthorTimeText(
                pubTime = "昨天 22:07",
                pubTs = 0L,
                nowMs = 1_700_003_480_000L
            )
        )
    }
}
