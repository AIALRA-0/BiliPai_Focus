package com.android.purebilibili.feature.home.subscription

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionFeedLayoutSpecTest {
    @Test
    fun articleImageFallbackMetricsRemainStable() {
        assertEquals(120.dp, SubscriptionFeedLayoutSpec.ImageErrorPlaceholderHeight)
        assertEquals(420.dp, SubscriptionFeedLayoutSpec.ArticleImageMaxHeight)
    }
}
