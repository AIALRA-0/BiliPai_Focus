package com.android.purebilibili.feature.login

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BilibiliLoginQrTest {
    private val key = "0123456789abcdef0123456789abcdef"
    private val web = "https://passport.bilibili.com/h5-app/passport/login/scan?qrcode_key=$key"
    private val tv = "https://passport.bilibili.com/x/passport-tv-login/h5/qrcode/auth?auth_code=$key"
    private val currentWeb = "https://account.bilibili.com/h5/account-h5/auth/scan-web?qrcode_key=$key"

    @Test
    fun `recognizes current computer login URL without executing its callback`() {
        val qr = assertNotNull(parseBilibiliLoginQr("$currentWeb&navhide=1&callback=evil%3A%2F%2Fexample"))
        assertEquals(BilibiliLoginQrType.WEB, qr.type)
        assertEquals("https://account.bilibili.com", qr.origin)
        assertEquals(currentWeb, qr.confirmationPage)
        assertEquals(key, qr.key)
        assertNull(parseBilibiliLoginQr(currentWeb.replace("account.bilibili.com", "account.bilibili.com.example.com")))
        assertNull(parseBilibiliLoginQr(currentWeb.replace("/auth/scan-web", "/auth/oauth")))
    }

    @Test
    fun `recognizes web and TV requests and discards unrelated redirect parameters`() {
        val request = assertNotNull(parseBilibiliLoginQr("$web&gourl=https%3A%2F%2Fexample.com"))
        assertEquals(BilibiliLoginQrType.WEB, request.type)
        assertEquals(key, request.key)
        assertEquals(web, request.confirmationPage)
        assertEquals(BilibiliLoginQrType.TV, assertNotNull(parseBilibiliLoginQr(tv)).type)
        assertEquals(key, extractTvAuthCode(tv))
        assertNull(extractTvAuthCode(web))
    }

    @Test
    fun `rejects unrelated codes ambiguous keys and spoofed login URLs`() {
        listOf(
            web.replace("https:", "http:"),
            web.replace("passport.bilibili.com", "passport.bilibili.com.example.com"),
            web.replace("passport.bilibili.com", "user@passport.bilibili.com"),
            web.replace("passport.bilibili.com", "passport.bilibili.com:8443"),
            web.replace("/login/scan", "/login/scan-extra"),
            "$web&qrcode_key=$key", "$web#fragment",
            web.replace(key, "short"), web.replace(key, "z".repeat(32)),
            tv.replace("auth_code", "not_auth_code"),
            "bilipai://transfer/request?payload=example",
            "https://www.bilibili.com/video/BV123", "not a url",
        ).forEach { assertNull(parseBilibiliLoginQr(it), it) }
    }

    @Test
    fun `rotates non square luminance frames using CameraX orientation`() {
        val pixels = byteArrayOf(1, 2, 3, 4, 5, 6)
        val clockwise = assertNotNull(rotateQrLuminance(pixels, 3, 2, 90))
        assertEquals(2, clockwise.width)
        assertEquals(3, clockwise.height)
        assertContentEquals(byteArrayOf(4, 1, 5, 2, 6, 3), clockwise.bytes)
        assertContentEquals(byteArrayOf(6, 5, 4, 3, 2, 1), assertNotNull(rotateQrLuminance(pixels, 3, 2, 180)).bytes)
        assertContentEquals(byteArrayOf(3, 6, 2, 5, 1, 4), assertNotNull(rotateQrLuminance(pixels, 3, 2, 270)).bytes)
        assertNull(rotateQrLuminance(pixels, 0, 2, 0))
        assertNull(rotateQrLuminance(pixels, 4, 2, 0))
    }

    @Test
    fun `decodes real web and TV QR images at all sensor rotations`() {
        for (payload in listOf(web, currentWeb, tv)) {
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240)
            val pixels = ByteArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) 0.toByte() else 255.toByte()
            }
            for (rotation in listOf(0, 90, 180, 270)) {
                assertEquals(payload, BiliPaiQrDecoder.decode(pixels, matrix.width, matrix.height, rotation))
            }
            val inverted = ByteArray(pixels.size) { pixels[it].toInt().inv().toByte() }
            assertEquals(payload, BiliPaiQrDecoder.decode(inverted, matrix.width, matrix.height))
        }
    }

    @Test
    fun `recognized unsupported QR is available for user feedback`() {
        val payload = "https://example.com/unsupported"
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240)
        val pixels = ByteArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) 0.toByte() else 255.toByte()
        }
        assertEquals(payload, BiliPaiQrDecoder.decodeRaw(pixels, matrix.width, matrix.height))
        assertNull(BiliPaiQrDecoder.decode(pixels, matrix.width, matrix.height))
    }
    @Test
    fun `album sampling preserves phone screenshot QR detail within the decode budget`() {
        assertEquals(1, resolveQrAlbumSampleSize(1080, 1920, 2048))
        assertEquals(2, resolveQrAlbumSampleSize(1440, 3200, 2048))
        assertEquals(2, resolveQrAlbumSampleSize(3200, 1440, 2048))
        assertEquals(4, resolveQrAlbumSampleSize(1440, 6000, 2048))
        assertEquals(1, resolveQrAlbumSampleSize(2048, 2048, 2048))
    }

    @Test
    fun `album sampling rejects unreadable image dimensions`() {
        assertNull(resolveQrAlbumSampleSize(-1, -1, 2048))
        assertNull(resolveQrAlbumSampleSize(0, 1920, 2048))
        assertNull(resolveQrAlbumSampleSize(1080, 1920, 0))
    }

}
