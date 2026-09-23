package com.android.purebilibili.feature.login

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** Decodes only BiliPai transfer payloads; normal web URLs are intentionally ignored. */
object BiliPaiQrDecoder {
    fun decode(bytes: ByteArray, width: Int, height: Int, rotation: Int = 0): String? {
        if (width <= 0 || height <= 0 || bytes.size < width * height) return null
        val source = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
        val rotated = if (rotation == 180) source.rotateCounterClockwise().rotateCounterClockwise() else source
        return try {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(rotated))).text
                .takeIf {
                    it.startsWith("bilipai://transfer/") ||
                        it.startsWith("https://passport.bilibili.com/x/passport-tv-login/h5/qrcode/auth")
                }
        } catch (_: ReaderException) {
            null
        }
    }
}

internal fun extractTvAuthCode(raw: String): String? {
    if (!raw.startsWith("https://passport.bilibili.com/x/passport-tv-login/h5/qrcode/auth")) return null
    return raw.substringAfter("auth_code=", "").substringBefore('&').takeIf { it.length in 16..128 }
}
