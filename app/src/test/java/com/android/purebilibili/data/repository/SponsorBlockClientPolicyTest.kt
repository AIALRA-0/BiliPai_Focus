package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.SponsorCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Request

class SponsorBlockClientPolicyTest {

    @Test
    fun sponsorBlockClientRequest_replacesSharedHeadersAndPreservesPayload() {
        val original = Request.Builder()
            .url("https://bsbsb.top/api/skipSegments?videoID=BV1xx411c7mD")
            .header("Origin", "https://www.bilibili.com")
            .header("User-Agent", "old-version")
            .build()

        val request = buildSponsorBlockClientRequest(original, "1.2.3-debug")

        assertEquals("https://github.com/jay3-yy/BiliPai", request.header("Origin"))
        assertEquals("1.2.3-debug", request.header("x-ext-version"))
        assertEquals("BiliPai/1.2.3-debug", request.header("User-Agent"))
        assertEquals(original.url, request.url)
        assertEquals(original.method, request.method)
        assertSame(original.body, request.body)
    }

    @Test
    fun sponsorBlockVoteBody_usesDocumentedVoteTypesAsJsonNumbers() {
        listOf(0, 1, 20).forEach { voteType ->
            val body = buildSponsorBlockVoteBody("private-user-id", "segment-id", voteType)

            assertEquals(
                buildJsonObject {
                    put("UUID", "segment-id")
                    put("userID", "private-user-id")
                    put("type", voteType)
                },
                Json.parseToJsonElement(body)
            )
        }
    }

    @Test
    fun sponsorBlockVoteBody_rejectsNegativeVoteType() {
        assertFailsWith<IllegalArgumentException> {
            buildSponsorBlockVoteBody("private-user-id", "segment-id", voteType = -1)
        }
    }

    @Test
    fun sponsorBlockVoteBody_categoryVoteOmitsType() {
        assertEquals(
            buildJsonObject {
                put("UUID", "segment-id")
                put("userID", "private-user-id")
                put("category", SponsorCategory.INTRO)
            },
            Json.parseToJsonElement(
                buildSponsorBlockVoteBody("private-user-id", "segment-id", category = SponsorCategory.INTRO)
            )
        )
    }

    @Test
    fun sponsorBlockVoteBody_requiresExactlyOneVoteKind() {
        assertFailsWith<IllegalArgumentException> {
            buildSponsorBlockVoteBody("private-user-id", "segment-id")
        }
        assertFailsWith<IllegalArgumentException> {
            buildSponsorBlockVoteBody("private-user-id", "segment-id", 0, SponsorCategory.INTRO)
        }
    }

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
