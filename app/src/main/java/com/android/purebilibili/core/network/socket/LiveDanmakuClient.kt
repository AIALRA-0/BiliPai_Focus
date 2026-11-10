package com.android.purebilibili.core.network.socket

import android.os.SystemClock
import android.util.Log
import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow

internal val LIVE_DANMAKU_AUTH_PROTOCOL_VERSION = DanmakuProtocol.PROTO_VER_BROTLI

/** Bilibili live danmaku socket with authentication, heartbeats and bounded message decoding. */
class LiveDanmakuClient(
    private val scope: CoroutineScope,
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val webSocketFactory: WebSocket.Factory = NetworkModule.okHttpClient,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "LiveDanmakuClient"
    // OkHttp callbacks, decoder and UI calls can run on different threads. All lifecycle
    // transitions and emissions share this lock so disconnect invalidates even in-flight decoding.
    private val connectionLock = Any()
    private class Connection {
        var socket: WebSocket? = null
        var authenticated = false
    }
    private data class IncomingFrame(val connection: Connection, val data: ByteArray)
    private var activeConnection: Connection? = null
    private val _isConnected = AtomicBoolean(false)
    val isConnected: Boolean get() = _isConnected.get()
    private var generation = 0L
    private var reconnectAllowed = false
    private var retryCount = 0
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var healthCheckJob: Job? = null
    private var connectionHealth = LiveDanmakuConnectionHealth()
    private var currentHostUrl = ""
    private var initialHostUrls: List<String> = emptyList()
    private var initialHostIndex = 0
    private var hasConnectedOnce = false
    private var currentAuthBody = ""
    private val incomingFrames = Channel<IncomingFrame>(24, BufferOverflow.DROP_OLDEST)
    private var decodeJob: Job? = null
    private val _messageFlow = MutableSharedFlow<DanmakuProtocol.Packet>(
        replay = 0,
        extraBufferCapacity = 200,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    companion object {
        private const val HEARTBEAT_INTERVAL = 30_000L
        private const val MAX_RETRY_DELAY = 10_000L
    }

    fun connect(url: String, token: String, roomId: Long, uid: Long = 0) {
        connect(listOf(url), token, roomId, uid)
    }

    /** Tries each server until authentication succeeds, then reconnects that host normally. */
    fun connect(urls: List<String>, token: String, roomId: Long, uid: Long = 0) {
        val candidates = urls.filter(String::isNotBlank).distinct()
        if (candidates.isEmpty()) return
        val authBody = JSONObject().apply {
            put("uid", uid)
            put("roomid", roomId)
            put("protover", LIVE_DANMAKU_AUTH_PROTOCOL_VERSION)
            put("platform", "web")
            put("type", 2)
            put("key", token)
        }.toString()
        synchronized(connectionLock) {
            generation++
            reconnectAllowed = true
            reconnectJob?.cancel()
            reconnectJob = null
            retryCount = 0
            initialHostUrls = candidates
            initialHostIndex = 0
            hasConnectedOnce = false
            currentHostUrl = candidates.first()
            currentAuthBody = authBody
            startDecodeLoop()
            internalConnect()
        }
    }

    private fun internalConnect() {
        closeCurrentConnection()
        if (!reconnectAllowed) return
        val connection = Connection()
        activeConnection = connection
        val request = Request.Builder()
            .url(currentHostUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .header("Origin", "https://live.bilibili.com")
            .build()
        val socket = webSocketFactory.newWebSocket(request, listenerFor(connection))
        // A factory can call its listener before returning; never resurrect a failed attempt.
        if (activeConnection === connection) connection.socket = socket else socket.cancel()
    }

    private fun listenerFor(connection: Connection) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronized(connectionLock) {
                if (activeConnection !== connection) {
                    webSocket.cancel()
                    return
                }
                connection.socket = webSocket
                _isConnected.set(true)
                connectionHealth = markLiveDanmakuConnected(connectionHealth, clockMs())
                Log.d(TAG, "WebSocket opened: $currentHostUrl")
                sendPacket(connection, DanmakuProtocol.Packet(
                    version = DanmakuProtocol.PROTO_VER_HEARTBEAT,
                    operation = DanmakuProtocol.OP_AUTH,
                    body = currentAuthBody.toByteArray(Charsets.UTF_8)
                ))
                if (activeConnection === connection) startHealthCheck(connection)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            synchronized(connectionLock) {
                if (activeConnection !== connection || !isConnected) return
                if (!shouldAcceptLiveDanmakuFrame(bytes.size)) return
                incomingFrames.trySend(IncomingFrame(connection, bytes.toByteArray()))
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            synchronized(connectionLock) {
                if (activeConnection !== connection) return
                // Reply to the peer's close frame; onClosed alone is never guaranteed.
                // 1005 denotes a valid empty close frame, but cannot itself be sent on the wire.
                closeCurrentConnection(if (code == 1005) 1000 else code, reason)
                scheduleReconnect()
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            synchronized(connectionLock) {
                if (activeConnection !== connection) return
                closeCurrentConnection()
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            synchronized(connectionLock) {
                if (activeConnection !== connection) return
                Log.w(TAG, "WebSocket failed: ${t.message}")
                closeCurrentConnection()
                scheduleReconnect()
            }
        }
    }

    fun disconnect() {
        synchronized(connectionLock) {
            generation++
            reconnectAllowed = false
            reconnectJob?.cancel()
            reconnectJob = null
            connectionHealth = markLiveDanmakuDisconnectedByUser(connectionHealth)
            closeCurrentConnection()
            decodeJob?.cancel()
            decodeJob = null
            while (incomingFrames.tryReceive().isSuccess) { /* Release queued old-room frames. */ }
        }
    }

    private fun closeCurrentConnection(code: Int = 1000, reason: String = "Normal Closure") {
        val previous = activeConnection
        activeConnection = null
        _isConnected.set(false)
        heartbeatJob?.cancel()
        heartbeatJob = null
        healthCheckJob?.cancel()
        healthCheckJob = null
        previous?.socket?.close(code, reason)
    }

    private fun startHeartbeat(connection: Connection) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(workerDispatcher) {
            while (isActive) {
                synchronized(connectionLock) {
                    if (activeConnection !== connection || !connection.authenticated) return@launch
                    sendPacket(connection, DanmakuProtocol.Packet(
                        version = DanmakuProtocol.PROTO_VER_HEARTBEAT,
                        operation = DanmakuProtocol.OP_HEARTBEAT,
                        body = "[object Object]".toByteArray(Charsets.UTF_8)
                    ))
                }
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private fun startHealthCheck(connection: Connection) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch(workerDispatcher) {
            while (isActive) {
                delay(LIVE_DANMAKU_HEALTH_CHECK_INTERVAL_MS)
                synchronized(connectionLock) {
                    if (activeConnection !== connection) return@launch
                    if (resolveLiveDanmakuHealthAction(connectionHealth, clockMs()) == LiveDanmakuHealthAction.RECONNECT) {
                        closeCurrentConnection(4000, "Silent Connection")
                        scheduleReconnect()
                        return@launch
                    }
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!reconnectAllowed || reconnectJob?.isActive == true) return
        if (!hasConnectedOnce && initialHostIndex + 1 < initialHostUrls.size) {
            initialHostIndex++
            currentHostUrl = initialHostUrls[initialHostIndex]
            internalConnect()
            return
        }
        val expectedGeneration = generation
        reconnectJob = scope.launch(workerDispatcher) {
            val delayMs = min(1000.0 * 2.0.pow(retryCount), MAX_RETRY_DELAY.toDouble()).toLong()
            delay(delayMs)
            synchronized(connectionLock) {
                if (generation != expectedGeneration || !reconnectAllowed) return@launch
                // A new attempt may fail immediately; it must be allowed to schedule its own retry.
                reconnectJob = null
                retryCount++
                internalConnect()
            }
        }
    }

    private fun sendPacket(connection: Connection, packet: DanmakuProtocol.Packet) {
        if (activeConnection !== connection) return
        if (connection.socket?.send(DanmakuProtocol.encode(packet).toByteString()) != true) {
            closeCurrentConnection()
            scheduleReconnect()
        }
    }

    private fun startDecodeLoop() {
        if (decodeJob?.isActive == true) return
        decodeJob = scope.launch(workerDispatcher) {
            for (frame in incomingFrames) {
                if (!synchronized(connectionLock) { activeConnection === frame.connection }) continue
                try {
                    val packets = DanmakuProtocol.decode(frame.data)
                    synchronized(connectionLock) {
                        for (packet in packets) {
                            if (activeConnection !== frame.connection) break
                            handlePacket(frame.connection, packet)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Log.w(TAG, "Message handling failed: ${e.message}")
                }
            }
        }
    }

    private fun handlePacket(connection: Connection, packet: DanmakuProtocol.Packet) {
        when (packet.operation) {
            DanmakuProtocol.OP_HEARTBEAT_REPLY -> {
                if (packet.body.size >= 4) {
                    connectionHealth = markLiveDanmakuHeartbeatReply(connectionHealth, clockMs())
                }
            }
            DanmakuProtocol.OP_AUTH_REPLY -> {
                val authCode = runCatching {
                    JSONObject(String(packet.body, Charsets.UTF_8)).getInt("code")
                }.getOrNull() ?: return // Damaged auth data is not an explicit server rejection.
                if (authCode == 0) {
                    connectionHealth = markLiveDanmakuServerFrameReceived(connectionHealth, clockMs())
                    if (!connection.authenticated) {
                        connection.authenticated = true
                        hasConnectedOnce = true
                        retryCount = 0
                        startHeartbeat(connection)
                    }
                } else {
                    Log.w(TAG, "Auth failed: code=$authCode")
                    reconnectAllowed = false
                    closeCurrentConnection(4001, "Auth Failed")
                }
            }
            DanmakuProtocol.OP_MESSAGE -> {
                if (!connection.authenticated) return
                connectionHealth = markLiveDanmakuBusinessMessage(connectionHealth, clockMs())
                _messageFlow.tryEmit(packet)
            }
        }
    }
}
