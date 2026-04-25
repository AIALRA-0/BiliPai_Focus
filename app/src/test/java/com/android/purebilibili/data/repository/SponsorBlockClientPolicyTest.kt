package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.SponsorCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SponsorBlockClientPolicyTest {

    @Test
    fun sponsorBlockClient_reusesSharedNetworkStack() {
        val sharedClient = NetworkModule.okHttpClient
        val sponsorClient = buildSponsorBlockHttpClient(sharedClient)

        assertEquals(sharedClient.protocols, sponsorClient.protocols)
        assertSame(sharedClient.cache, sponsorClient.cache)
        assertEquals(5_000, sponsorClient.connectTimeoutMillis.toLong())
        assertEquals(5_000, sponsorClient.readTimeoutMillis.toLong())
    }

    @Test
    fun sponsorBlockSegmentsUrl_includesCidWhenAvailable() {
        val url = buildSponsorBlockSegmentsUrl(
            baseUrl = "https://bsbsb.top/api",
            bvid = "BV1xx411c7mD",
            cid = 1234L,
            categories = listOf(SponsorCategory.INTRO, SponsorCategory.SPONSOR)
        )

        assertEquals(
            "https://bsbsb.top/api/skipSegments?videoID=BV1xx411c7mD&cid=1234&category=intro&category=sponsor",
            url
        )
    }

    @Test
    fun sponsorBlockSegmentsUrl_omitsCidWhenMissing() {
        val url = buildSponsorBlockSegmentsUrl(
            baseUrl = "https://bsbsb.top/api",
            bvid = "BV1xx411c7mD",
            cid = 0L,
            categories = listOf(SponsorCategory.INTRO)
        )

        assertEquals(
            "https://bsbsb.top/api/skipSegments?videoID=BV1xx411c7mD&category=intro",
            url
        )
    }
}
