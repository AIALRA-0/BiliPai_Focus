package com.android.purebilibili.feature.nearby

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLSocket

/** 一个 TLS 流承载两个方向的请求。读取循环不等待用户确认或播放器准备。 */
internal class NearbyDuplexConnection(
    val peer: NearbyPeer,
    private val socket: Closeable,
    inputStream: InputStream,
    outputStream: OutputStream,
    private val ownId: String,
    private val handle: suspend (NearbyDuplexConnection, NearbyMessage) -> NearbyMessage,
) {
    constructor(peer: NearbyPeer, socket: SSLSocket, ownId: String,
        handle: suspend (NearbyDuplexConnection, NearbyMessage) -> NearbyMessage) :
        this(peer, socket, socket.inputStream, socket.outputStream, ownId, handle)

    private val output = DataOutputStream(outputStream)
    private val input = DataInputStream(inputStream)
    private val writes = Mutex()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<NearbyMessage>>()
    private val slots = Semaphore(4)
    @Volatile var pairingApproved = false
    private val failure = AtomicReference<IOException?>(null)
    val closed: Boolean get() = failure.get() != null

    suspend fun request(message: NearbyMessage): NearbyMessage = withContext(Dispatchers.IO) {
        failure.get()?.let { throw it }
        val requestId = nearbyToken()
        val answer = CompletableDeferred<NearbyMessage>()
        check(pending.size < 8) { "接力请求过多" }
        pending[requestId] = answer
        // close 与请求注册可能并发；关闭后的请求不能继续等待读取循环。
        failure.get()?.let { pending.remove(requestId); throw it }
        val finished = AtomicBoolean(false)
        // 独立关闭任务也覆盖阻塞式写入；仅取消 await 无法解除系统 socket 写入。
        val deadline = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            try {
                delay(60_000)
                close(NearbyConnectionFailure("接力请求超时，请保持对端前台并确认请求后重新发送"))
            }
            finally { if (!finished.get()) close() }
        }
        try {
            write(message.copy(requestId = requestId, replyTo = ""))
            withTimeout(60_000) { answer.await() }
        } finally {
            pending.remove(requestId)
            // 超时或取消后终止远端准备，防止迟到的请求继续启动播放。
            if (!answer.isCompleted) close()
            finished.set(true)
            deadline.cancel()
        }
    }

    suspend fun run() = withNearbySocket(socket) {
        try {
            coroutineScope {
                while (true) {
                    val message = input.readNearby()
                    require(message.id == peer.id)
                    if (message.replyTo.isNotEmpty()) {
                        pending[message.replyTo]?.complete(message)
                    } else {
                        require(message.requestId.isNotEmpty())
                        if (!slots.tryAcquire()) throw IOException("接力请求过多")
                        launch {
                            try {
                                val reply = handle(this@NearbyDuplexConnection, message)
                                write(reply.copy(id = ownId, requestId = "", replyTo = message.requestId))
                            } finally { slots.release() }
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            close(error)
            throw error
        } finally { close() }
    }

    private suspend fun write(message: NearbyMessage) = writes.withLock { output.writeNearby(message) }

    fun close(cause: Exception? = null) {
        val error = cause as? IOException ?: IOException("设备连接已断开", cause)
        if (!failure.compareAndSet(null, error)) return
        runCatching { socket.close() }
        pending.values.forEach { it.completeExceptionally(error) }
        pending.clear()
    }
}

/** 双端比较 128 位身份摘要；设备名称和局域网广播均不作为信任凭据。 */
internal fun nearbyVerificationCode(firstPin: String, secondPin: String): String =
    nearbyPin((listOf(firstPin, secondPin).sorted().joinToString(":")).toByteArray(Charsets.US_ASCII))
        .take(32).uppercase().chunked(4).joinToString(" ")
