package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.Collections

class AicuRepositoryTest {
    @Test fun `uncached queries enqueue stream then consume their own ticket`() = runTest {
        val paths = Collections.synchronizedList(mutableListOf<String>())
        val positions = Collections.synchronizedList(mutableListOf<Int?>())
        val client = buildAicuClient().newBuilder().addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            paths.add(path)
            val body = when {
                path.endsWith("enqueue") -> {
                    assertEquals("POST", request.method)
                    """{"code":0,"data":{"ticket":"test-ticket","status":"waiting","position":3}}"""
                }
                path.endsWith("stream") -> {
                    assertEquals("test-ticket", request.url.queryParameter("ticket"))
                    "event: position\ndata: {\"ahead\":1}\n\nevent: ready\ndata: {}\n\n"
                }
                else -> {
                    assertEquals("/api/v4/search/getreply", path)
                    assertEquals("test-ticket", request.url.queryParameter("ticket"))
                    """{"code":0,"data":{"replies":[],"cursor":{"all_count":0,"is_end":true}}}"""
                }
            }
            val type = if (path.endsWith("stream")) "text/event-stream" else "application/json"
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", type).body(body.toResponseBody(type.toMediaType())).build()
        }.build()
        val result = AicuRepository(client).query(AicuQuery(2, AicuCategory.COMMENT)) { positions.add(it) }
        assertTrue(result.isEnd)
        assertEquals(listOf("/api/v4/queue/enqueue", "/api/v4/queue/stream", "/api/v4/search/getreply"), paths.toList())
        assertEquals(listOf(2, 1, null), positions.toList())
    }

    @Test fun `cookies are host isolated and expired cookies are discarded`() {
        val api = "https://api.aicu.cc/".toHttpUrl()
        val jar = AicuCookieJar()
        val cookie = Cookie.Builder().name("ASession").value("test").domain("aicu.cc").path("/").build()
        jar.saveFromResponse(api, listOf(cookie))
        assertEquals(listOf(cookie), jar.loadForRequest(api))
        assertTrue(jar.loadForRequest("https://api.bilibili.com/".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("https://other.aicu.cc/".toHttpUrl()).isEmpty())
        jar.saveFromResponse(api, listOf(Cookie.Builder().name("ASession").value("expired").domain("aicu.cc").path("/").expiresAt(1).build()))
        assertTrue(jar.loadForRequest(api).isEmpty())
    }
}
