package com.android.purebilibili.feature.login

import java.security.KeyPairGenerator
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BiliPaiTransferCodecTest {
    @Test
    fun request_roundTrips_andValidatesReceiverKeyFingerprint() {
        val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val fingerprint = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(key.public.encoded))
        val request = BiliPaiTransferRequest(
            transferId = UUID.randomUUID().toString(),
            receiverDeviceId = fingerprint,
            receiverPublicKey = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.public.encoded),
            expiresAt = System.currentTimeMillis() + 60_000,
        )
        assertEquals(request, BiliPaiTransferCodec.decodeRequest(BiliPaiTransferCodec.encodeRequest(request)))
    }

    @Test
    fun expiredRequest_isRejected() {
        val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val request = BiliPaiTransferRequest("id", "device", java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(key.public.encoded), 0L)
        assertThrows(Exception::class.java) {
            BiliPaiTransferCodec.decodeRequest(BiliPaiTransferCodec.encodeRequest(request))
        }
    }
}
