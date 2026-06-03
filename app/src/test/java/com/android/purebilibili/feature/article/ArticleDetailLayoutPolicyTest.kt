package com.android.purebilibili.feature.article

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleDetailLayoutPolicyTest {

    @Test
    fun `resolveArticleDetailBottomPadding adds navigation bar and extra spacing`() {
        assertEquals(40.dp, resolveArticleDetailBottomPadding(navigationBarsBottom = 16.dp))
    }

    @Test
    fun `resolveArticleDetailBottomPadding clamps negative inputs to zero`() {
        assertEquals(0.dp, resolveArticleDetailBottomPadding(navigationBarsBottom = (-10).dp, extraBottomPadding = (-6).dp))
    }

    @Test
    fun `resolveArticleImageAspectRatio keeps full tall image ratio`() {
        assertEquals(0.25f, resolveArticleImageAspectRatio(width = 900, height = 3600))
    }

    @Test
    fun `resolveArticleImageAspectRatio returns null for missing dimensions`() {
        assertEquals(null, resolveArticleImageAspectRatio(width = 0, height = 3600))
    }
}
