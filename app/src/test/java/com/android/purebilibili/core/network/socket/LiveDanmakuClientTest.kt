package com.android.purebilibili.core.network.socket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LiveDanmakuClientTest {
    @Test
    fun `auth identifies room and user and heartbeat waits for successful reply`() = runBlocking {
        Fixture().use { f ->
            val socket = f.connect(roomId = 123, uid = 456)
            socket.open()
            val auth = withTimeout(5_000) { socket.sent.receive() }
            assertEquals(DanmakuProtocol.OP_AUTH, auth.operation)
            assertEquals(DanmakuProtocol.PROTO_VER_HEARTBEAT, auth.version)
            val json = JSONObject(String(auth.body, Charsets.UTF_8))
            assertEquals(123L, json.getLong("roomid"))
            assertEquals(456L, json.getLong("uid"))
            assertEquals(3, json.getInt("protover"))
            assertEquals("web", json.getString("platform"))
            assertEquals(2, json.getInt("type"))
            assertEquals("token", json.getString("key"))
            assertTrue(socket.sent.tryReceive().isFailure)
            socket.reply(8, "{\"code\":0}")
            assertEquals(2, withTimeout(5_000) { socket.sent.receive() }.operation)
            // Callback takes the lifecycle lock after authentication finishes scheduling its timer.
            socket.bytes(byteArrayOf())
            f.scheduler.advanceTimeBy(30_000)
            f.scheduler.runCurrent()
            assertEquals(2, socket.sent.tryReceive().getOrThrow().operation)
        }
    }

    @Test
    fun `old socket callbacks cannot fail or publish into new room`() = runBlocking {
        Fixture().use { f ->
            val old = f.connect()
            f.authenticate(old)
            val current = f.connect(roomId = 2)
            f.authenticate(current)
            old.reply(5, "{\"cmd\":\"OLD_ROOM\"}")
            old.reply(8, "{\"code\":-101}")
            old.listener.onFailure(old, IOException("old failure"), null)
            old.listener.onClosing(old, 1000, "old closing")
            old.listener.onClosed(old, 1000, "old closed")
            old.open()
            current.reply(5, "{\"cmd\":\"CURRENT_ROOM\"}")
            val message = withTimeout(5_000) { f.messages.receive() }
            assertEquals("{\"cmd\":\"CURRENT_ROOM\"}", String(message.body))
            assertTrue(f.messages.tryReceive().isFailure)
            assertTrue(f.client.isConnected)
            assertTrue(current.closes.isEmpty())
            f.scheduler.advanceTimeBy(30_000)
            f.scheduler.runCurrent()
            assertEquals(2, f.sockets.size)
            assertTrue(old.sent.tryReceive().isFailure)
            assertEquals(2, current.sent.tryReceive().getOrThrow().operation)
        }
    }

    @Test
    fun `queued old auth frame is ignored after switching rooms`() {
        Fixture(queuedDecoder = true).use { f ->
            val old = f.connect()
            old.open()
            old.reply(8, "{\"code\":-101}")
            val current = f.connect(roomId = 2)
            current.open()
            f.scheduler.runCurrent()
            assertTrue(f.client.isConnected)
            assertTrue(current.closes.isEmpty())
            assertEquals(DanmakuProtocol.OP_AUTH, current.sent.tryReceive().getOrThrow().operation)
            assertTrue(current.sent.tryReceive().isFailure)
        }
    }

    @Test
    fun `disconnect rejects late open and frames and client can connect again`() = runBlocking {
        Fixture().use { f ->
            val old = f.connect()
            f.client.disconnect()
            old.open()
            old.reply(8, "{\"code\":0}")
            old.reply(5, "old")
            old.listener.onFailure(old, IOException("late"), null)
            f.scheduler.advanceTimeBy(80_000)
            f.scheduler.runCurrent()
            assertFalse(f.client.isConnected)
            assertEquals(1, f.sockets.size)
            assertTrue(f.messages.tryReceive().isFailure)
            assertTrue(old.sent.tryReceive().isFailure)
            val current = f.connect(roomId = 2)
            f.authenticate(current)
            current.reply(5, "new")
            assertEquals("new", String(withTimeout(5_000) { f.messages.receive() }.body))
        }
    }

    @Test
    fun `remote normal close is acknowledged and restored once`() = runBlocking {
        Fixture().use { f ->
            val old = f.connect()
            f.authenticate(old)
            old.listener.onClosing(old, 1000, "server maintenance")
            assertFalse(f.client.isConnected)
            assertEquals(listOf(1000 to "server maintenance"), old.closes)
            old.listener.onClosed(old, 1000, "server maintenance")
            f.scheduler.advanceTimeBy(1_000)
            f.scheduler.runCurrent()
            assertEquals(2, f.sockets.size)
            val current = f.sockets.last()
            f.authenticate(current)
            assertTrue(f.client.isConnected)
        }
    }

    @Test
    fun `remote empty close frame is acknowledged with sendable normal code`() {
        Fixture().use { f ->
            val socket = f.connect()
            socket.open()
            socket.listener.onClosing(socket, 1005, "")
            assertFalse(f.client.isConnected)
            assertEquals(listOf(1000 to ""), socket.closes)
            f.scheduler.advanceTimeBy(1_000)
            f.scheduler.runCurrent()
            assertEquals(2, f.sockets.size)
        }
    }

    @Test
    fun `explicit auth rejection stops heartbeat and reconnect immediately`() = runBlocking {
        Fixture().use { f ->
            val socket = f.connect()
            socket.open()
            socket.sent.receive()
            socket.reply(8, "{\"code\":-101}")
            withTimeout(5_000) { socket.closeSignal.await() }
            assertFalse(f.client.isConnected)
            assertEquals(4001, socket.closes.single().first)
            f.scheduler.advanceTimeBy(80_000)
            f.scheduler.runCurrent()
            assertEquals(1, f.sockets.size)
            assertTrue(socket.sent.tryReceive().isFailure)
        }
    }

    @Test
    fun `damaged auth and compression do not kill later valid frames`() = runBlocking {
        Fixture().use { f ->
            val socket = f.connect()
            socket.open()
            socket.sent.receive()
            socket.reply(8, "not json")
            socket.bytes(DanmakuProtocol.encode(DanmakuProtocol.Packet(2, 5, body = byteArrayOf(1, 2))))
            socket.reply(8, "{\"code\":0}")
            assertEquals(2, withTimeout(5_000) { socket.sent.receive() }.operation)
            socket.reply(5, "still alive")
            assertEquals("still alive", String(withTimeout(5_000) { f.messages.receive() }.body))
            assertTrue(socket.closes.isEmpty())
        }
    }

    @Test
    fun `invalid frames cannot keep silent connection alive`() = runBlocking {
        Fixture().use { f ->
            val socket = f.connect()
            f.authenticate(socket)
            f.scheduler.advanceTimeBy(70_000)
            f.scheduler.runCurrent()
            socket.bytes(ByteArray(16))
            socket.bytes(ByteArray(MAX_LIVE_DANMAKU_FRAME_BYTES + 1))
            f.scheduler.advanceTimeBy(10_000)
            f.scheduler.runCurrent()
            assertEquals(4000, socket.closes.single().first)
            assertFalse(f.client.isConnected)
            f.scheduler.advanceTimeBy(1_000)
            f.scheduler.runCurrent()
            assertEquals(2, f.sockets.size)
        }
    }

    @Test
    fun `initial server failure advances host and ignores its delayed callbacks`() {
        Fixture().use { f ->
            f.client.connect(listOf("wss://first.test/sub", "wss://second.test/sub"), "token", 1)
            val first = f.sockets.single()
            first.listener.onFailure(first, IOException("unreachable"), null)
            assertEquals("second.test", f.sockets.last().request().url.host)
            val second = f.sockets.last()
            second.open()
            first.listener.onFailure(first, IOException("late again"), null)
            assertTrue(f.client.isConnected)
            assertTrue(second.closes.isEmpty())
            assertEquals(2, f.sockets.size)
        }
    }

    @Test
    fun `failed send triggers recovery without waiting for health timeout`() {
        Fixture().use { f ->
            val socket = f.connect()
            socket.acceptSends = false
            socket.open()
            assertFalse(f.client.isConnected)
            f.scheduler.advanceTimeBy(1_000)
            f.scheduler.runCurrent()
            assertEquals(2, f.sockets.size)
        }
    }

    private class Fixture(queuedDecoder: Boolean = false) : Closeable {
        val scheduler = TestCoroutineScheduler()
        private val dispatcher = if (queuedDecoder) StandardTestDispatcher(scheduler) else UnconfinedTestDispatcher(scheduler)
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sockets = Collections.synchronizedList(mutableListOf<FakeSocket>())
        val messages = Channel<DanmakuProtocol.Packet>(Channel.UNLIMITED)
        val client = LiveDanmakuClient(scope, { scheduler.currentTime }, object : WebSocket.Factory {
            override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket =
                FakeSocket(request, listener).also { sockets.add(it) }
        }, dispatcher)

        init {
            scope.launch(UnconfinedTestDispatcher(scheduler)) { client.messageFlow.collect { messages.send(it) } }
        }

        fun connect(roomId: Long = 1, uid: Long = 0): FakeSocket {
            client.connect("wss://live.test/sub", "token", roomId, uid)
            return sockets.last()
        }

        suspend fun authenticate(socket: FakeSocket) {
            socket.open()
            assertEquals(7, withTimeout(5_000) { socket.sent.receive() }.operation)
            socket.reply(8, "{\"code\":0}")
            assertEquals(2, withTimeout(5_000) { socket.sent.receive() }.operation)
            socket.bytes(byteArrayOf()) // Wait for the auth handler to release the lifecycle lock.
        }

        override fun close() {
            client.disconnect()
            scope.cancel()
        }
    }

    /** Controlled local peer: real listener callbacks and wire packets, without network timing. */
    private class FakeSocket(private val socketRequest: Request, val listener: WebSocketListener) : WebSocket {
        val sent = Channel<DanmakuProtocol.Packet>(Channel.UNLIMITED)
        val closes = Collections.synchronizedList(mutableListOf<Pair<Int, String?>>())
        val closeSignal = CompletableDeferred<Unit>()
        var acceptSends = true
        override fun request() = socketRequest
        override fun queueSize() = 0L
        override fun send(text: String) = false
        override fun send(bytes: ByteString): Boolean {
            if (!acceptSends) return false
            val buffer = ByteBuffer.wrap(bytes.toByteArray())
            buffer.int
            val headerSize = buffer.short.toInt() and 0xffff
            val version = buffer.short.toInt() and 0xffff
            val operation = buffer.int
            val sequence = buffer.int
            val body = bytes.toByteArray().copyOfRange(headerSize, bytes.size)
            sent.trySend(DanmakuProtocol.Packet(version, operation, sequence, body))
            return true
        }
        override fun close(code: Int, reason: String?): Boolean {
            closes.add(code to reason)
            closeSignal.complete(Unit)
            return true
        }
        override fun cancel() = Unit
        fun open() = listener.onOpen(this, Response.Builder().request(socketRequest)
            .protocol(Protocol.HTTP_1_1).code(101).message("Switching Protocols").build())
        fun reply(operation: Int, body: String) = bytes(DanmakuProtocol.encode(
            DanmakuProtocol.Packet(1, operation, body = body.toByteArray(Charsets.UTF_8))
        ))
        fun bytes(data: ByteArray) = listener.onMessage(this, data.toByteString())
    }
}
