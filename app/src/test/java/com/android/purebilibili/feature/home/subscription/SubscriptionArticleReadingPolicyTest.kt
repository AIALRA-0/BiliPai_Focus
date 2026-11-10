package com.android.purebilibili.feature.home.subscription

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionArticleReadingPolicyTest {
    @Test
    fun `sharing includes article identity and only a usable web link`() {
        assertEquals("标题\n订阅源\nhttps://example.com/article",
            buildSubscriptionArticleShareText("标题", "订阅源", "https://example.com/article"))
        assertEquals("标题", buildSubscriptionArticleShareText("标题", "", "javascript:alert(1)"))
        assertEquals("标题\n订阅源", buildSubscriptionArticleShareText("标题", "订阅源", ""))
    }
}
