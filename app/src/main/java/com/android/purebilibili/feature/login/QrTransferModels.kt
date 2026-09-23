package com.android.purebilibili.feature.login

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BiliPaiTransferRequest(
    val version: Int = 1,
    val transferId: String,
    val receiverDeviceId: String,
    val receiverPublicKey: String,
    val expiresAt: Long,
)

@Serializable
data class BiliPaiTransferEnvelope(
    val version: Int = 1,
    val transferId: String,
    val senderDeviceId: String,
    val senderPublicKey: String,
    val wrappedKey: String,
    val iv: String,
    val ciphertext: String,
    val signature: String,
    val expiresAt: Long,
)

@Serializable
data class BiliPaiSessionBundle(
    val mid: Long,
    val sessData: String,
    val csrf: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenPlatform: String = "tv",
    val buvid3: String = "",
    val isVip: Boolean = false,
)

object BiliPaiTransferCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encodeRequest(request: BiliPaiTransferRequest): String =
        "bilipai://transfer/request?payload=${encode(json.encodeToString(BiliPaiTransferRequest.serializer(), request))}"

    fun decodeRequest(raw: String, now: Long = System.currentTimeMillis()): BiliPaiTransferRequest {
        require(raw.length <= MAX_QR_CHARS) { "二维码内容过大" }
        require(raw.startsWith("bilipai://transfer/request?payload=")) { "不是 BiliPai 传输二维码" }
        val request = json.decodeFromString(
            BiliPaiTransferRequest.serializer(), decode(raw.substringAfter("payload=")))
        require(request.version == 1 && request.transferId.isNotBlank()) { "传输请求版本无效" }
        require(request.expiresAt > now) { "传输请求已过期" }
        require(publicKey(request.receiverPublicKey).fingerprint() == request.receiverDeviceId) {
            "接收设备标识校验失败"
        }
        return request
    }

    fun encodeEnvelope(envelope: BiliPaiTransferEnvelope): String =
        "bilipai://transfer/envelope?payload=${encode(json.encodeToString(BiliPaiTransferEnvelope.serializer(), envelope))}"

    fun decodeEnvelope(raw: String): BiliPaiTransferEnvelope {
        require(raw.length <= MAX_QR_CHARS) { "二维码内容过大" }
        require(raw.startsWith("bilipai://transfer/envelope?payload=")) { "不是 BiliPai 加密传输二维码" }
        return json.decodeFromString(
            BiliPaiTransferEnvelope.serializer(), decode(raw.substringAfter("payload=")))
    }

    private fun publicKey(value: String): PublicKey = KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec(Base64.getUrlDecoder().decode(value)))

    private fun PublicKey.fingerprint(): String = Base64.getUrlEncoder().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(encoded))

    private fun encode(value: String): String = Base64.getUrlEncoder().encodeToString(
        value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(
        Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private const val MAX_QR_CHARS = 100_000
}
