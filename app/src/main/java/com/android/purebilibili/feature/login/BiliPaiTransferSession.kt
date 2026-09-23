package com.android.purebilibili.feature.login

import android.content.Context
import com.android.purebilibili.core.store.AccountSessionStore
import com.android.purebilibili.core.store.TokenManager
import java.security.KeyPair
import java.util.UUID

enum class BiliPaiTransferMode { RECEIVE, SEND }

sealed interface BiliPaiTransferState {
    data object Idle : BiliPaiTransferState
    data class WaitingForRequest(val request: BiliPaiTransferRequest) : BiliPaiTransferState
    data class RequestReady(val request: BiliPaiTransferRequest) : BiliPaiTransferState
    data class EnvelopeReady(val envelope: BiliPaiTransferEnvelope) : BiliPaiTransferState
    data class AwaitingConfirmation(val bundle: BiliPaiSessionBundle) : BiliPaiTransferState
    data class Completed(val mid: Long) : BiliPaiTransferState
    data class Failed(val message: String) : BiliPaiTransferState
}

/** Coordinates the two QR scans; it contains no camera or Compose code. */
class BiliPaiTransferSession(private val context: Context) {
    private val keys: KeyPair by lazy { BiliPaiTransferKeyStore.getOrCreate(context) }
    private var request: BiliPaiTransferRequest? = null
    var state: BiliPaiTransferState = BiliPaiTransferState.Idle
        private set

    fun beginReceive(now: Long = System.currentTimeMillis()): BiliPaiTransferRequest {
        val created = BiliPaiTransferCodecRequestFactory.create(context, now)
        request = created
        state = BiliPaiTransferState.RequestReady(created)
        return created
    }

    fun acceptRequest(raw: String): BiliPaiTransferRequest {
        val parsed = BiliPaiTransferCodec.decodeRequest(raw)
        request = parsed
        state = BiliPaiTransferState.WaitingForRequest(parsed)
        return parsed
    }

    fun createEnvelope(bundle: BiliPaiSessionBundle): BiliPaiTransferEnvelope {
        val target = request ?: error("请先扫描接收设备请求")
        val envelope = BiliPaiTransferCrypto.encrypt(keys, keys.public.fingerprint(), target, bundle)
        state = BiliPaiTransferState.EnvelopeReady(envelope)
        return envelope
    }

    fun acceptEnvelope(raw: String): BiliPaiSessionBundle {
        val target = request ?: error("请先创建或扫描传输请求")
        val envelope = BiliPaiTransferCodec.decodeEnvelope(raw)
        val bundle = BiliPaiTransferCrypto.decrypt(keys.private, target, envelope)
        state = BiliPaiTransferState.AwaitingConfirmation(bundle)
        return bundle
    }

    suspend fun confirmImport(bundle: BiliPaiSessionBundle): Boolean {
        val ok = AccountSessionStore.importTransferredSession(context, bundle)
        state = if (ok) BiliPaiTransferState.Completed(bundle.mid)
        else BiliPaiTransferState.Failed("账号会话导入失败")
        return ok
    }

    fun clear() { request = null; state = BiliPaiTransferState.Idle }
}

private object BiliPaiTransferCodecRequestFactory {
    fun create(context: Context, now: Long): BiliPaiTransferRequest =
        BiliPaiTransferRequest(
            transferId = UUID.randomUUID().toString(),
            receiverDeviceId = BiliPaiTransferKeyStore.getOrCreate(context).public.fingerprint(),
            receiverPublicKey = android.util.Base64.encodeToString(
                BiliPaiTransferKeyStore.getOrCreate(context).public.encoded,
                android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE),
            expiresAt = now + 10 * 60 * 1000L,
        )
}

private fun java.security.PublicKey.fingerprint(): String = android.util.Base64.encodeToString(
    java.security.MessageDigest.getInstance("SHA-256").digest(encoded),
    android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
