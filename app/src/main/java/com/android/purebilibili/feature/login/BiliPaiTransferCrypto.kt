package com.android.purebilibili.feature.login

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json

/** Encrypts a session bundle for the receiver key carried by a request QR. */
object BiliPaiTransferCrypto {
    private const val RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES = "AES/GCM/NoPadding"
    private const val SIGNATURE = "SHA256withRSA"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encrypt(
        sender: KeyPair,
        senderDeviceId: String,
        request: BiliPaiTransferRequest,
        bundle: BiliPaiSessionBundle,
        now: Long = System.currentTimeMillis(),
    ): BiliPaiTransferEnvelope {
        require(request.expiresAt > now) { "传输请求已过期" }
        require(bundle.sessData.length <= MAX_SESSION_FIELD_LENGTH) { "会话数据过大" }
        val aes = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES).apply {
            init(Cipher.ENCRYPT_MODE, aes, GCMParameterSpec(128, iv))
        }
        val ciphertext = cipher.doFinal(
            json.encodeToString(BiliPaiSessionBundle.serializer(), bundle)
                .toByteArray(StandardCharsets.UTF_8))
        val wrapped = Cipher.getInstance(RSA).run {
            init(Cipher.ENCRYPT_MODE, publicKey(request.receiverPublicKey))
            doFinal(aes.encoded)
        }
        val senderPublic = b64(sender.public.encoded)
        val canonical = canonical(request.transferId, senderDeviceId, senderPublic,
            b64(wrapped), b64(iv), b64(ciphertext), request.expiresAt)
        val signature = Signature.getInstance(SIGNATURE).run {
            initSign(sender.private); update(canonical.toByteArray(StandardCharsets.UTF_8)); sign()
        }
        return BiliPaiTransferEnvelope(1, request.transferId, senderDeviceId, senderPublic,
            b64(wrapped), b64(iv), b64(ciphertext), b64(signature), request.expiresAt)
    }

    fun decrypt(receiverPrivate: PrivateKey, request: BiliPaiTransferRequest,
                envelope: BiliPaiTransferEnvelope, now: Long = System.currentTimeMillis()): BiliPaiSessionBundle {
        require(envelope.version == 1 && envelope.transferId == request.transferId) { "传输会话不匹配" }
        require(envelope.expiresAt > now) { "传输数据已过期" }
        require(envelope.ciphertext.length <= MAX_QR_FIELD_LENGTH) { "加密数据过大" }
        val sender = publicKey(envelope.senderPublicKey)
        val canonical = canonical(envelope.transferId, envelope.senderDeviceId, envelope.senderPublicKey,
            envelope.wrappedKey, envelope.iv, envelope.ciphertext, envelope.expiresAt)
        require(Signature.getInstance(SIGNATURE).run {
            initVerify(sender); update(canonical.toByteArray(StandardCharsets.UTF_8)); verify(unb64(envelope.signature))
        }) { "传输签名校验失败" }
        val aes = Cipher.getInstance(RSA).run {
            init(Cipher.DECRYPT_MODE, receiverPrivate); doFinal(unb64(envelope.wrappedKey))
        }
        val plaintext = Cipher.getInstance(AES).run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(aes, "AES"), GCMParameterSpec(128, unb64(envelope.iv)))
            doFinal(unb64(envelope.ciphertext))
        }
        return json.decodeFromString(BiliPaiSessionBundle.serializer(), String(plaintext, StandardCharsets.UTF_8))
    }

    private fun publicKey(value: String): PublicKey = java.security.KeyFactory.getInstance("RSA")
        .generatePublic(java.security.spec.X509EncodedKeySpec(unb64(value)))
    private fun canonical(vararg values: Any): String = values.joinToString(".")
    private fun b64(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP or Base64.URL_SAFE)
    private fun unb64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)

    private const val MAX_SESSION_FIELD_LENGTH = 16_384
    private const val MAX_QR_FIELD_LENGTH = 80_000
}
