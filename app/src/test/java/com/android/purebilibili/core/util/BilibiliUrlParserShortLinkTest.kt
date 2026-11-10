package com.android.purebilibili.core.util

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the b23.tv redirect decoding used by link routing (share intents, in-app links).
 * A local responder stands in for b23.tv so relative/absolute Location handling stays verified
 * without hitting the network.
 */
class BilibiliUrlParserShortLinkTest {

    private suspend fun withRedirectServer(
        respond: (HttpExchange) -> Unit,
        block: suspend (baseUrl: String) -> Unit
    ) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            respond(exchange)
            exchange.close()
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}/WKzSIzs")
        } finally {
            server.stop(0)
        }
    }

    private fun HttpExchange.redirect(location: String) {
        responseHeaders.add("Location", location)
        sendResponseHeaders(302, -1)
    }

    @Test
    fun `resolveShortUrl returns absolute redirect target`() = runBlocking {
        withRedirectServer({ it.redirect("https://www.bilibili.com/video/BV1FbKP6aEsY") }) { baseUrl ->
            assertEquals(
                "https://www.bilibili.com/video/BV1FbKP6aEsY",
                BilibiliUrlParser.resolveShortUrl(baseUrl)
            )
        }
    }

    @Test
    fun `resolveShortUrl completes relative redirect target`() = runBlocking {
        withRedirectServer({ it.redirect("/video/BV1FbKP6aEsY") }) { baseUrl ->
            val expectedHost = baseUrl.substringBefore("/WKzSIzs")
            assertEquals(
                "$expectedHost/video/BV1FbKP6aEsY",
                BilibiliUrlParser.resolveShortUrl(baseUrl)
            )
        }
    }

    @Test
    fun `resolveShortUrl trims trailing slash of redirect target`() = runBlocking {
        withRedirectServer({ it.redirect("https://www.bilibili.com/video/BV1FbKP6aEsY/") }) { baseUrl ->
            assertEquals(
                "https://www.bilibili.com/video/BV1FbKP6aEsY",
                BilibiliUrlParser.resolveShortUrl(baseUrl)
            )
        }
    }

    @Test
    fun `resolveShortUrl supports servers that require a browser user agent`() = runBlocking {
        val target = "https://www.bilibili.com/video/BV1FbKP6aEsY"
        withRedirectServer({ exchange ->
            if (exchange.requestHeaders.getFirst("User-Agent")?.startsWith("Mozilla/") == true) {
                exchange.redirect(target)
            } else {
                exchange.sendResponseHeaders(403, -1)
            }
        }) { baseUrl ->
            assertEquals(target, BilibiliUrlParser.resolveShortUrl(baseUrl))
        }
    }

    @Test
    fun `resolveShortUrl ignores responses without redirect location`() = runBlocking {
        withRedirectServer({ it.sendResponseHeaders(200, -1) }) { baseUrl ->
            assertNull(BilibiliUrlParser.resolveShortUrl(baseUrl))
        }
        withRedirectServer({ it.sendResponseHeaders(404, -1) }) { baseUrl ->
            assertNull(BilibiliUrlParser.resolveShortUrl(baseUrl))
        }
    }

    @Test
    fun `resolveShortUrl returns null for unreachable short link`() = runBlocking {
        assertNull(BilibiliUrlParser.resolveShortUrl("http://127.0.0.1:1/closed"))
    }
}
