package com.android.purebilibili.feature.nearby

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom

internal const val NEARBY_TITLE = "设备接力"
internal const val NEARBY_SERVICE = "_bilipai._tcp."
internal const val NEARBY_PORT = 39327
internal val nearbyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
internal data class NearbyPeer(
    val id: String,
    val name: String,
    val pin: String,
    val host: String,
    val port: Int,
)

@Serializable
internal data class NearbyVideo(
    val bvid: String,
    val cid: Long,
    val positionMs: Long,
    val speed: Float,
    val title: String,
    val cover: String,
) {
    fun validate() {
        require(Regex("BV[0-9A-Za-z]{10}", RegexOption.IGNORE_CASE).matches(bvid))
        require(cid > 0 && positionMs in 0..604_800_000L)
        require(speed.isFinite() && speed in 0.25f..4f)
        require(title.length <= 200 && cover.length <= 1024)
        require(cover.isBlank() || URI(cover).let { it.scheme == "https" && it.userInfo == null &&
            it.port == -1 && (it.host == "hdslb.com" || it.host?.endsWith(".hdslb.com") == true) })
    }
}

@Serializable
internal data class NearbyMessage(
    val version: Int = 2,
    val requestId: String = "",
    val replyTo: String = "",
    val transferId: String = "",
    val kind: String,
    val id: String,
    val name: String = "",
    val token: String = "",
    val video: NearbyVideo? = null,
    val peer: NearbyPeer? = null,
    val accepted: Boolean = false,
)

internal data class NearbyPairCode(val peer: NearbyPeer, val token: String) {
    fun encode(): String = Uri.Builder().scheme("bilipai-pair").authority("v1")
        .appendQueryParameter("host", peer.host).appendQueryParameter("port", peer.port.toString())
        .appendQueryParameter("id", peer.id).appendQueryParameter("name", peer.name)
        .appendQueryParameter("pin", peer.pin).appendQueryParameter("token", token).build().toString()

    companion object {
        fun decode(text: String): NearbyPairCode {
            require(text.length in 1..2048) { "配对码格式不正确" }
            val uri = Uri.parse(text.trim())
            require(uri.scheme == "bilipai-pair" && uri.host == "v1") { "不是 BiliPai 配对码" }
            val host = uri.getQueryParameter("host").orEmpty()
            requireLocalAddress(host)
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 0
            require(port in 1024..65535) { "连接端口不正确" }
            val pin = uri.getQueryParameter("pin").orEmpty()
            val token = uri.getQueryParameter("token").orEmpty()
            val id = uri.getQueryParameter("id").orEmpty()
            require(Regex("[a-f0-9]{64}").matches(pin) && Regex("[a-f0-9]{32}").matches(token))
            require(id == pin.take(32))
            return NearbyPairCode(NearbyPeer(id, uri.getQueryParameter("name").orEmpty().take(40), pin, host, port), token)
        }
    }
}

/** 仅接受 IP 字面量，禁止由收到的配对码触发任意域名解析或公网连接。 */
internal fun requireLocalAddress(host: String) {
    require(host.length in 3..64 && host.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it in ".:" })
    require(host.contains(':') || (host.split('.').size == 4 &&
        host.split('.').all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }))
    val address = InetAddress.getByName(host)
    require(!address.isAnyLocalAddress && !address.isMulticastAddress &&
        (address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            (address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)))
}

internal fun ByteArray.nearbyHex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
internal fun nearbyToken(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.nearbyHex()
internal fun nearbyPin(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).nearbyHex()

internal fun normalizeNearbyCover(cover: String): String = runCatching {
    val normalized = when {
        cover.startsWith("//") -> "https:$cover"
        cover.startsWith("http://") -> "https://" + cover.removePrefix("http://")
        else -> cover
    }
    val uri = URI(normalized)
    normalized.takeIf { it.length <= 1024 && uri.scheme == "https" && uri.userInfo == null &&
        uri.port == -1 && (uri.host == "hdslb.com" || uri.host?.endsWith(".hdslb.com") == true) }.orEmpty()
}.getOrDefault("")

internal fun DataInputStream.readNearby(): NearbyMessage {
    val size = readInt()
    require(size in 1..16_384) { "接力消息过大" }
    val bytes = ByteArray(size).also(::readFully)
    return nearbyJson.decodeFromString<NearbyMessage>(bytes.toString(Charsets.UTF_8)).also {
        if (it.version != 2) throw NearbyConnectionFailure("两端设备接力协议不兼容，请使用相同版本")
        require(it.requestId.length <= 32 && it.replyTo.length <= 32 &&
            it.transferId.length <= 32 && it.id.length in 1..64 && it.name.length <= 40 && it.token.length <= 64)
    }
}

internal fun DataOutputStream.writeNearby(message: NearbyMessage) {
    val bytes = nearbyJson.encodeToString(NearbyMessage.serializer(), message).toByteArray(Charsets.UTF_8)
    require(bytes.size <= 16_384)
    writeInt(bytes.size)
    write(bytes)
    flush()
}
