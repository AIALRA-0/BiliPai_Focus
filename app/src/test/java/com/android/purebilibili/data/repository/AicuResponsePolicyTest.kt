package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.*
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.ZoneId

class AicuResponsePolicyTest {
    @Test fun `UID rejects malformed zero and overflowing values`() {
        listOf("", "0", "-1", "1e3", "1.0", "9223372036854775808").forEach { assertNull(parseAicuUid(it)) }
        assertEquals(123L, parseAicuUid(" 00123 "))
    }

    @Test fun `query encodes filters without allowing extra query parameters`() {
        val url = buildAicuQueryUrl(AicuQuery(2, AicuCategory.COMMENT, filter = AicuFilter(keyword = "a&uid=3", commentMode = 2)), "ticket")
        assertEquals("2", url.queryParameter("uid"))
        assertEquals("a&uid=3", url.queryParameter("keyword"))
        assertEquals("2", url.queryParameter("mode"))
        assertEquals("100", url.queryParameter("ps"))
        assertNull(buildAicuQueryUrl(AicuQuery(2, AicuCategory.LIVE_DANMAKU), "t").queryParameter("mode"))
    }

    @Test fun `date range includes the local end of day across daylight saving`() {
        val range = AicuFilter(startDate = "2026-03-08", endDate = "2026-03-08").timestamps(ZoneId.of("America/New_York"))
        assertEquals(23 * 3600L, requireNotNull(range.second) - requireNotNull(range.first) + 1)
        assertEquals(null to null, AicuFilter().timestamps())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inverted dates cannot reach the network`() {
        AicuFilter(startDate = "2026-03-09", endDate = "2026-03-08").timestamps()
    }

    @Test fun `comments retain large IDs and root IDs without double conversion`() {
        val page = decodeAicuPage(decodeAicuEnvelope("""{"code":0,"data":{"replies":[{"rpid":310971854512,"message":"test","time":123,"rank":2,"dyn":{"oid":1226750626638069766,"type":17},"parent":{"rootid":"306765041841"},"future":true}],"cursor":{"all_count":1,"is_end":true}}}"""), AicuQuery(2, AicuCategory.COMMENT))
        assertEquals("1226750626638069766", page.records.single().objectId)
        assertEquals("306765041841", page.records.single().rootId)
        assertTrue(page.isEnd)
    }

    @Test fun `video and grouped live responses use their own wire fields`() {
        val video = decodeAicuPage(decodeAicuEnvelope("""{"code":0,"data":{"videodmlist":[{"id":"9","oid":10,"content":"hello","ctime":1,"progress":1250}],"cursor":{"all_count":1}}}"""), AicuQuery(2, AicuCategory.VIDEO_DANMAKU))
        assertEquals(1250L, video.records.single().progressMs)
        val live = decodeAicuPage(decodeAicuEnvelope("""{"code":0,"data":{"list":[{"roominfo":{"roomid":42,"roomname":"room","upname":"up"},"danmu":[{"ts":1,"text":"one"},{"ts":2,"text":"two"}]}],"cursor":{"is_end":true}}}"""), AicuQuery(2, AicuCategory.LIVE_DANMAKU))
        assertEquals(2, live.records.size)
        assertEquals("42", live.records.first().roomId)
        assertEquals(live.records.first().groupKey, live.records.last().groupKey)
        assertNull(live.total)
    }

    @Test fun `queue parser supports heartbeat multiline data and ready without data`() {
        val decoder = AicuQueueEventDecoder()
        assertNull(decoder.accept(": heartbeat"))
        decoder.accept("event: position")
        decoder.accept("data: {\"ahead\":")
        decoder.accept("data: 4}")
        assertEquals(AicuQueueEvent("position", "{\"ahead\":\n4}"), decoder.accept(""))
        decoder.accept("event: ready")
        assertEquals(AicuQueueEvent("ready", ""), decoder.accept(""))
    }

    @Test fun `rate limiting supports seconds and HTTP dates`() {
        assertEquals(120L, aicuRetryAfterSeconds("120"))
        assertEquals(60L, aicuRetryAfterSeconds("Mon, 31 Aug 2026 00:01:00 GMT", Instant.parse("2026-08-31T00:00:00Z")))
        assertTrue(aicuHttpFailure(468, null).message.orEmpty().contains("安全验证"))
    }

    @Test(expected = AicuRequestException::class)
    fun `expired ticket is an error rather than empty history`() {
        decodeAicuEnvelope("""{"code":-419,"data":null}""")
    }

    @Test(expected = AicuRequestException::class)
    fun `changed record schema is not reported as empty results`() {
        decodeAicuPage(decodeAicuEnvelope("""{"code":0,"data":{}}"""), AicuQuery(2, AicuCategory.COMMENT))
    }

    @Test fun `client has no inherited interceptors persistence or redirects`() {
        val client = buildAicuClient()
        assertTrue(client.interceptors.isEmpty())
        assertTrue(client.networkInterceptors.isEmpty())
        assertNull(client.cache)
        assertFalse(client.followRedirects)
        assertTrue(client.cookieJar is AicuCookieJar)
    }
}
