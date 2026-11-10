package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.WbiImg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeWebFeedWbiPolicyTest {

    @Test
    fun prefersCachedWbiKeysWhenAvailable() {
        val cached = "cached-img" to "cached-sub"
        val result = resolveHomeFeedWbiKeys(
            cachedKeys = cached,
            navWbiImg = WbiImg(
                img_url = "https://i0.hdslb.com/bfs/wbi/nav-img.png",
                sub_url = "https://i0.hdslb.com/bfs/wbi/nav-sub.png"
            )
        )

        assertEquals(cached, result)
    }

    @Test
    fun fallsBackToNavWbiImageWhenCacheMissing() {
        val result = resolveHomeFeedWbiKeys(
            cachedKeys = null,
            navWbiImg = WbiImg(
                img_url = "https://i0.hdslb.com/bfs/wbi/nav-img.png",
                sub_url = "https://i0.hdslb.com/bfs/wbi/nav-sub.png"
            )
        )

        assertEquals("nav-img" to "nav-sub", result)
    }

    @Test
    fun returnsNullWhenNoWbiSourceExists() {
        assertNull(
            resolveHomeFeedWbiKeys(
                cachedKeys = null,
                navWbiImg = null
            )
        )
    }
    @Test
    fun refreshAndPaginationKeepTheProtocolVersionAndAdvanceTogether() {
        val initial = buildHomeWebFeedParams(idx = 0, refreshCount = 20)
        val next = buildHomeWebFeedParams(idx = 3, refreshCount = 35)
        assertEquals("V8", initial["feed_version"])
        assertEquals(initial["feed_version"], next["feed_version"])
        assertEquals("0", initial["fresh_idx"])
        assertEquals("3", next["fresh_idx"])
        assertEquals(next["fresh_idx"], next["brush"])
        assertEquals("35", next["ps"])
        assertEquals("4", next["fresh_type"])
    }

}
