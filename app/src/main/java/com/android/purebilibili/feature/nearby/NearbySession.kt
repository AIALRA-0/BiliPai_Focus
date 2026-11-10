package com.android.purebilibili.feature.nearby

import android.content.Context
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

internal data class NearbyApproval(val peer: NearbyPeer, val answer: CompletableDeferred<Boolean>,
    val verificationCode: String = "")
internal data class NearbyIncoming(val id: String, val peer: NearbyPeer, val video: NearbyVideo,
    val answer: CompletableDeferred<Boolean>,
    val latest: suspend () -> NearbyVideo)
internal data class NearbyState(
    val receiving: Boolean = false,
    val connectedPins: Set<String> = emptySet(),
    val peers: List<NearbyPeer> = emptyList(),
    val discovered: List<NearbyPeer> = emptyList(),
    val pairingCode: String? = null,
    val approval: NearbyApproval? = null,
    val incoming: NearbyIncoming? = null,
    val busy: Boolean = false,
    val status: String = "开启后可与附近的 BiliPai 互相接力",
    val localEndpoint: String? = null,
)

/** 网络任务由宿主协程启动和取消；关闭接收会同时关闭监听与活动连接。 */
internal class NearbySession(context: Context) {
    private val context = context.applicationContext
    private val preferences = context.getSharedPreferences("nearby_trusted_devices_v1", Context.MODE_PRIVATE)
    private val identity by lazy { NearbyIdentity() }
    private val mutableState = MutableStateFlow(NearbyState())
    val state = mutableState.asStateFlow()
    private val connections = Collections.newSetFromMap(ConcurrentHashMap<SSLSocket, Boolean>())
    private val links = ConcurrentHashMap<String, NearbyDuplexConnection>()
    private val pendingPeers = ConcurrentHashMap<NearbyDuplexConnection, NearbyPeer>()
    private data class SnapshotSource(val pin: String, val read: suspend () -> NearbyVideo)
    private val snapshots = ConcurrentHashMap<String, SnapshotSource>()
    @Volatile private var ownerScope: CoroutineScope? = null
    @Volatile private var allowNearbyPairing = false
    private val incomingLock = Mutex()
    private val outgoingLock = Mutex()
    private val peersLock = Any()
    private val pairingLock = Any()
    private var pairingToken: String? = null
    private var pairingExpiresAt = 0L
    @Volatile private var advertisedEndpoint: Pair<String, Int>? = null
    @Volatile private var server: SSLServerSocket? = null
    private var discovery: NearbyDiscovery? = null
    private val deviceName get() = Build.MODEL.take(40)

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val ownPin = identity.pin
        // 身份密钥未随安装恢复时，旧的信任记录不继续复用。
        val stored = preferences.getString("identity", null)
        val peers = if (stored == ownPin) try {
            nearbyJson.decodeFromString<List<NearbyPeer>>(preferences.getString("peers", "[]") ?: "[]")
        } catch (_: IllegalArgumentException) { emptyList() } else emptyList()
        if (stored != ownPin) preferences.edit().clear().putString("identity", ownPin).commit()
        val trusted = peers.filter { peer ->
            runCatching { validatePeer(peer) }.isSuccess
        }.distinctBy { it.pin }.take(16)
        mutableState.update { it.copy(peers = trusted) }
    }

    suspend fun listen() = coroutineScope {
        ownerScope = this
        val listener = withContext(Dispatchers.IO) {
            (identity.tls { pin -> isTrusted(pin) || pairingOpen() || allowNearbyPairing }.serverSocketFactory
                .createServerSocket(NEARBY_PORT, 4) as SSLServerSocket).apply {
                enabledProtocols = supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
                needClientAuth = true
                soTimeout = 1000
            }
        }
        server = listener
        val localEndpoint = withContext(Dispatchers.IO) { runCatching { "${localAddress()}:$NEARBY_PORT" }.getOrNull() }
        mutableState.update { it.copy(receiving = true, localEndpoint = localEndpoint,
            status = "已开启，正在发现局域网设备") }
        val networkDiscovery = NearbyDiscovery(context, identity.pin, deviceName, NEARBY_PORT) { peers ->
            mutableState.update { it.copy(discovered = peers) }
        }
        discovery = networkDiscovery
        networkDiscovery.start()
        val slots = Semaphore(4)
        try {
            withContext(Dispatchers.IO) {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val socket = try { listener.accept() as SSLSocket } catch (_: SocketTimeoutException) { continue }
                    if (!slots.tryAcquire()) { socket.close(); continue }
                    connections += socket
                    launch {
                        try { withNearbySocket(socket) { acceptConnection(socket) } }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* 未认证或中断的连接不改变播放器状态。 */ }
                        finally { connections -= socket; socket.close(); slots.release() }
                    }
                }
            }
        } finally {
            networkDiscovery.close()
            close()
        }
    }

    fun close() {
        ownerScope = null
        links.values.toList().forEach { it.close() }
        links.clear()
        snapshots.clear()
        pendingPeers.clear()
        discovery?.close()
        discovery = null
        runCatching { server?.close() }
        server = null
        advertisedEndpoint = null
        connections.toList().forEach { runCatching { it.close() } }
        cancelPairing()
        state.value.approval?.answer?.complete(false)
        state.value.incoming?.answer?.complete(false)
        mutableState.update { it.copy(receiving = false, localEndpoint = null,
            approval = null, incoming = null, discovered = emptyList(), connectedPins = emptySet()) }
    }

    fun status(message: String) { mutableState.update { it.copy(status = message) } }

    suspend fun localPeer(): NearbyPeer = withContext(Dispatchers.IO) {
        check(server != null) { "请先开启设备接力" }
        val endpoint = advertisedEndpoint ?: (localAddress() to NEARBY_PORT)
        NearbyPeer(identity.pin.take(32), deviceName, identity.pin, endpoint.first, endpoint.second)
    }

    fun setPairingVisible(visible: Boolean) { allowNearbyPairing = visible }

    private suspend fun confirmIdentity(peer: NearbyPeer): Boolean {
        check(incomingLock.tryLock()) { "正在处理另一台设备的请求" }
        val approval = NearbyApproval(peer, CompletableDeferred(), nearbyVerificationCode(identity.pin, peer.pin))
        try {
            mutableState.update { it.copy(approval = approval) }
            return withTimeout(55_000) { approval.answer.await() }
        } finally { mutableState.update { it.copy(approval = null) }; incomingLock.unlock() }
    }

    private fun validatePeer(peer: NearbyPeer) {
        require(Regex("[a-f0-9]{64}").matches(peer.pin) && peer.id == peer.pin.take(32) && peer.pin != identity.pin)
        require(peer.name.length in 1..40 && peer.port in 1024..65535)
        requireLocalAddress(peer.host)
    }

    suspend fun createPairingCode(hostOverride: String = ""): String = withContext(Dispatchers.IO) {
        check(server != null) { "请先开启设备接力" }
        val endpoint = if (hostOverride.isBlank()) localAddress() to NEARBY_PORT else {
            val host = hostOverride.substringBeforeLast(':')
            host to (hostOverride.substringAfterLast(':').toIntOrNull() ?: 0)
        }
        requireLocalAddress(endpoint.first)
        require(endpoint.second in 1024..65535) { "请填写 IP:端口" }
        advertisedEndpoint = endpoint
        val token = nearbyToken()
        synchronized(pairingLock) { pairingToken = token; pairingExpiresAt = SystemClock.elapsedRealtime() + 300_000L }
        NearbyPairCode(NearbyPeer(identity.pin.take(32), deviceName, identity.pin, endpoint.first, endpoint.second), token)
            .encode().also { code -> mutableState.update { it.copy(pairingCode = code) } }
    }

    fun cancelPairing() {
        synchronized(pairingLock) { pairingToken = null; pairingExpiresAt = 0 }
        mutableState.update { it.copy(pairingCode = null) }
    }

    suspend fun pair(codeText: String) = withOutgoing {
        val code = NearbyPairCode.decode(codeText)
        pairPeer(code.peer, code.token)
    }

    suspend fun pairNearby(peer: NearbyPeer) = withOutgoing { pairPeer(peer, "") }

    private suspend fun pairPeer(peer: NearbyPeer, token: String) = coroutineScope {
        validatePeer(peer)
        val link = connect(peer, enrollment = true)
        var confirmed = false
        try {
            // 两端同时展示身份摘要，双方均确认后才提交信任。
            val remote = async {
                link.request(message("pair").copy(token = token, peer = localPeer()))
            }
            val localAccepted = confirmIdentity(peer)
            if (!localAccepted) throw NearbyConnectionFailure("已取消配对")
            val reply = remote.await()
            if (reply.kind == "pair_expired") throw NearbyConnectionFailure("对端配对码已失效，请重新生成完整配对码")
            if (reply.kind != "pair_ready" || !reply.accepted) {
                throw NearbyConnectionFailure("配对未确认；请在两端核对相同的身份校验码")
            }
            val committed = link.request(message("pair_commit"))
            check(committed.kind == "paired" && committed.accepted) { "对端未完成身份确认" }
            savePeer(peer)
            confirmed = true
            status("已与 ${peer.name} 配对，双向连接已建立")
        } finally { if (!confirmed) link.close() }
    }

    suspend fun connectTrusted(peer: NearbyPeer) = withOutgoing {
        require(isTrusted(peer.pin)) { "设备已解除配对" }
        val destination = state.value.discovered.firstOrNull { it.pin == peer.pin } ?: peer
        val link = connect(destination, enrollment = false)
        val reply = link.request(message("hello"))
        check(reply.kind == "hello" && reply.accepted) { "对端未确认设备身份" }
        status("已连接 ${peer.name}，可双向接力")
    }

    suspend fun send(peer: NearbyPeer, video: NearbyVideo, latest: suspend () -> NearbyVideo): Boolean = withOutgoing {
        video.validate()
        require(isTrusted(peer.pin)) { "设备已解除配对" }
        val destination = state.value.discovered.firstOrNull { it.pin == peer.pin } ?: peer
        val link = links[peer.pin]?.takeUnless { it.closed } ?: connect(destination, enrollment = false)
        val transferId = nearbyToken()
        snapshots[transferId] = SnapshotSource(peer.pin, latest)
        try {
            val result = link.request(message("video").copy(video = video, transferId = transferId))
            require(result.kind == "playing" && result.transferId == transferId) { "对方未确认开始播放" }
            status(if (result.accepted) "${peer.name} 已开始播放" else "对方未接收，当前设备继续播放")
            result.accepted
        } finally { snapshots.remove(transferId) }
    }

    suspend fun forget(peer: NearbyPeer) = withContext(Dispatchers.IO) {
        links.remove(peer.pin)?.close()
        publishConnections()
        synchronized(peersLock) {
            val updated = state.value.peers.filterNot { it.pin == peer.pin }
            persistPeers(updated)
        }
    }

    private suspend fun <T> withOutgoing(block: suspend () -> T): T {
        check(outgoingLock.tryLock()) { "正在连接另一台设备" }
        mutableState.update { it.copy(busy = true, status = "正在建立加密连接…") }
        try { return withContext(Dispatchers.IO) { block() } }
        finally { mutableState.update { it.copy(busy = false) }; outgoingLock.unlock() }
    }

    private fun message(kind: String) = NearbyMessage(kind = kind, id = identity.pin.take(32), name = deviceName)

    private suspend fun connect(peer: NearbyPeer, enrollment: Boolean): NearbyDuplexConnection {
        links[peer.pin]?.takeUnless { it.closed }?.let { return it }
        requireLocalAddress(peer.host)
        require(peer.port in 1024..65535)
        val scope = checkNotNull(ownerScope) { "请先开启设备接力" }
        val socket = identity.tls { it == peer.pin && (enrollment || isTrusted(it)) }
            .socketFactory.createSocket() as SSLSocket
        connections += socket
        var stage = NearbyConnectionStage.CONNECT
        try {
            status("正在连接 ${peer.name}…")
            socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
            socket.soTimeout = 10_000
            socket.connect(InetSocketAddress(peer.host, peer.port), 5000)
            stage = NearbyConnectionStage.HANDSHAKE
            status("地址已连通，正在核对加密身份…")
            socket.startHandshake()
            currentCoroutineContext().ensureActive()
            check(ownerScope === scope) { "设备接力已关闭" }
            status(if (enrollment) "加密连接已建立，请在两端核对身份校验码" else "加密连接已建立，等待对端响应")
            // 连接移交给监听生命周期；请求结束不会关闭双向流。
            socket.soTimeout = 0
            val link = NearbyDuplexConnection(peer, socket, identity.pin.take(32), ::handle)
            // 在返回前注册，调用者可立即通过同一连接发起请求。
            val existing = links.putIfAbsent(peer.pin, link)
            if (existing != null && !existing.closed) {
                socket.close(); connections -= socket
                return existing
            }
            if (existing != null) links[peer.pin] = link
            scope.launch(Dispatchers.IO) { runLink(link, socket) }
            return link
        } catch (cancelled: CancellationException) {
            socket.close(); connections -= socket; throw cancelled
        } catch (error: Exception) {
            socket.close(); connections -= socket
            throw NearbyConnectionFailure(nearbyConnectionHint(error, stage), error)
        }
    }

    private suspend fun acceptConnection(socket: SSLSocket) {
        socket.soTimeout = 10_000
        socket.startHandshake()
        val certificate = socket.session.peerCertificates.single() as X509Certificate
        val pin = nearbyPin(certificate.publicKey.encoded)
        val peer = state.value.peers.firstOrNull { it.pin == pin } ?: NearbyPeer(
            pin.take(32), "附近设备", pin, socket.inetAddress.hostAddress.orEmpty(), NEARBY_PORT)
        socket.soTimeout = 0
        runLink(NearbyDuplexConnection(peer, socket, identity.pin.take(32), ::handle), socket)
    }

    private suspend fun runLink(link: NearbyDuplexConnection, socket: SSLSocket) = coroutineScope {
        // 已建立的连接优先复用，避免另一端地址无法反向访问。
        val previous = links.putIfAbsent(link.peer.pin, link)
        if (previous != null && previous !== link && !previous.closed) {
            link.close(); connections -= socket; return@coroutineScope
        }
        if (previous != null) links[link.peer.pin] = link
        publishConnections()
        val enrollmentDeadline = launch {
            delay(120_000)
            if (!isTrusted(link.peer.pin)) link.close()
        }
        try { link.run() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* 断线只结束本连接，不取消监听与其他设备。 */ }
        finally {
            enrollmentDeadline.cancel()
            link.close()
            links.remove(link.peer.pin, link)
            pendingPeers.remove(link)
            connections -= socket
            publishConnections()
        }
    }

    private fun publishConnections() {
        mutableState.update { it.copy(connectedPins = links.values.filterNot { link -> link.closed }
            .map { link -> link.peer.pin }.toSet()) }
    }

    private suspend fun handle(link: NearbyDuplexConnection, request: NearbyMessage): NearbyMessage {
        val pin = link.peer.pin
        return when (request.kind) {
            "hello" -> {
                require(isTrusted(pin))
                message("hello").copy(accepted = true)
            }
            "pair" -> {
                require(allowNearbyPairing || pairingOpen()) { "对端未打开设备接力面板" }
                val peer = requireNotNull(request.peer)
                validatePeer(peer)
                require(peer.pin == pin)
                if (request.token.isNotEmpty()) {
                    val valid = synchronized(pairingLock) {
                        val token = pairingToken
                        if (token != null && pairingExpiresAt > SystemClock.elapsedRealtime() &&
                            MessageDigest.isEqual(token.toByteArray(), request.token.toByteArray())) {
                            pairingToken = null; true
                        } else false
                    }
                    if (!valid) return message("pair_expired")
                    mutableState.update { it.copy(pairingCode = null) }
                }
                val accepted = confirmIdentity(peer)
                link.pairingApproved = accepted
                // 保存对端自报的可连接地址，身份始终使用 TLS 证书校验。
                if (accepted) pendingPeers[link] = peer
                message("pair_ready").copy(accepted = accepted)
            }
            "pair_commit" -> {
                val peer = pendingPeers.remove(link)
                val accepted = link.pairingApproved && peer != null
                if (accepted) {
                    savePeer(requireNotNull(peer))
                    status("已与 ${peer.name} 配对，双向连接已建立")
                }
                link.pairingApproved = false
                message("paired").copy(accepted = accepted)
            }
            "snapshot" -> {
                require(isTrusted(pin))
                val source = snapshots.remove(request.transferId)?.also { require(it.pin == pin) }
                val video = source?.read?.invoke()?.also { it.validate() }
                message("snapshot").copy(transferId = request.transferId, video = video, accepted = video != null)
            }
            "video" -> {
                require(isTrusted(pin) && request.transferId.length == 32)
                val video = requireNotNull(request.video).also { it.validate() }
                check(incomingLock.tryLock())
                val peer = state.value.peers.first { it.pin == pin }
                val incoming = NearbyIncoming(request.transferId, peer, video, CompletableDeferred()) {
                    val reply = link.request(message("snapshot").copy(transferId = request.transferId))
                    require(!incomingEnded(request.transferId) && reply.kind == "snapshot" && reply.accepted && reply.transferId == request.transferId)
                    requireNotNull(reply.video).also {
                        it.validate()
                        require(it.bvid == video.bvid && it.cid == video.cid)
                    }
                }
                try {
                    mutableState.update { it.copy(incoming = incoming) }
                    val playing = withTimeout(55_000) { incoming.answer.await() }
                    message("playing").copy(transferId = request.transferId, accepted = playing && isTrusted(pin))
                } finally {
                    incoming.answer.complete(false)
                    mutableState.update { it.copy(incoming = null) }
                    incomingLock.unlock()
                }
            }
            else -> error("不支持的接力消息")
        }
    }

    private fun incomingEnded(id: String) = state.value.incoming?.let { it.id != id || it.answer.isCompleted } != false

    private fun pairingOpen() = synchronized(pairingLock) {
        pairingToken != null && pairingExpiresAt > SystemClock.elapsedRealtime()
    }
    private fun isTrusted(pin: String) = state.value.peers.any { it.pin == pin }
    private fun savePeer(peer: NearbyPeer) = synchronized(peersLock) {
        persistPeers((state.value.peers.filterNot { it.pin == peer.pin } + peer).takeLast(16))
    }
    private fun persistPeers(peers: List<NearbyPeer>) {
        check(preferences.edit().putString("peers", nearbyJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(NearbyPeer.serializer()), peers)).commit())
        mutableState.update { it.copy(peers = peers) }
    }
    private fun localAddress(): String = NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("wifi")) 0 else 1 }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>().firstOrNull { it.isSiteLocalAddress }?.hostAddress
        ?: error("未找到局域网地址，请连接 Wi-Fi 后重试")
}
