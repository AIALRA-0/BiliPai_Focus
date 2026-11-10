package com.android.purebilibili.feature.plugin

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CdnRangeRequestTest {
    private val client = OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build()

    @Test
    fun `partial response returns exactly the requested bytes`() = runTest {
        withServer { socket -> respond(socket, 206, "bytes 10-13/100", "abcd") }.use { server ->
            val sample = readExactCdnRange(client, server.url, CdnByteRange(10, 13))
            assertContentEquals("abcd".encodeToByteArray(), sample.bytes)
            assertTrue(sample.elapsedMs >= sample.firstByteMs)
        }
    }

    @Test
    fun `server ignoring range is rejected`() = runTest {
        withServer { socket -> respond(socket, 200, "bytes 10-13/100", "abcd") }.use { server ->
            assertFailsWith<IOException> { readExactCdnRange(client, server.url, CdnByteRange(10, 13)) }
        }
    }

    @Test
    fun `shifted truncated and oversized bodies are rejected`() = runTest {
        listOf("bytes 0-3/100" to "abcd", "bytes 10-13/100" to "abc", "bytes 10-13/100" to "abcde").forEach { (header, body) ->
            withServer { socket -> respond(socket, 206, header, body) }.use { server ->
                assertFailsWith<IOException> { readExactCdnRange(client, server.url, CdnByteRange(10, 13)) }
            }
        }
    }

    @Test
    fun `canceling a caller closes a stalled socket`() = runTest {
        val accepted = CountDownLatch(1)
        val disconnected = CountDownLatch(1)
        withServer { socket ->
            accepted.countDown()
            if (socket.getInputStream().read() == -1) disconnected.countDown()
        }.use { server ->
            val request = launch { readExactCdnRange(client, server.url, CdnByteRange(10, 13)) }
            runCurrent()
            assertTrue(accepted.await(2, TimeUnit.SECONDS))
            request.cancelAndJoin()
            assertTrue(disconnected.await(2, TimeUnit.SECONDS))
        }
    }

    private fun respond(socket: Socket, status: Int, range: String, body: String) {
        socket.getOutputStream().write(
            "HTTP/1.1 $status Response\r\nContent-Range: $range\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body".encodeToByteArray()
        )
        socket.getOutputStream().flush()
    }

    private fun withServer(handler: (Socket) -> Unit) = TestServer(handler)

    private class TestServer(handler: (Socket) -> Unit) : AutoCloseable {
        private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val url = "http://127.0.0.1:${server.localPort}/media.m4s"
        private var peer: Socket? = null
        private val worker = thread(isDaemon = true, name = "cdn-range-test") {
            try {
                server.accept().use { socket ->
                    peer = socket
                    socket.soTimeout = 3_000
                    val reader = socket.getInputStream().bufferedReader()
                    while (!reader.readLine().isNullOrEmpty()) { /* Consume HTTP headers. */ }
                    handler(socket)
                }
            } catch (_: IOException) {
                // Closing a failed or canceled test tears down pending I/O.
            }
        }
        override fun close() {
            peer?.close()
            server.close()
            worker.join(3_000)
        }
    }
}
