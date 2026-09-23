package com.android.purebilibili.feature.login

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime

/**
 * Login identity used by PiliPlus' Android-HD Passport requests.
 *
 * `buvid3` is a browser-cookie identifier and must not be substituted for the
 * Android-HD `buvid` that is included in the signed Passport form and header.
 */
internal data class PiliPlusLoginIdentity(
    val buvid: String,
    val deviceId: String,
)

internal fun createPiliPlusLoginIdentity(
    now: LocalDateTime = LocalDateTime.now(),
    random: SecureRandom = SecureRandom(),
): PiliPlusLoginIdentity {
    val buvidSeed = ByteArray(16).also(random::nextBytes)
    val buvidDigest = md5Hex(buvidSeed)
    val buvid = "XY${buvidDigest[2]}${buvidDigest[12]}${buvidDigest[22]}$buvidDigest"

    val deviceBytes = buildList {
        repeat(16) { add(random.nextInt(256)) }
        add(toBcd(now.year / 100))
        add(toBcd(now.year % 100))
        add(toBcd(now.monthValue))
        add(toBcd(now.dayOfMonth))
        add(toBcd(now.hour))
        add(toBcd(now.minute))
        add(toBcd(now.second))
        repeat(8) { add(random.nextInt(256)) }
    }
    val deviceId = md5Hex(deviceBytes.map(Int::toByte).toByteArray()) +
        (deviceBytes.sum() and 0xFF).toString(16).padStart(2, '0')

    return PiliPlusLoginIdentity(buvid = buvid, deviceId = deviceId)
}

internal fun createPiliPlusRandomString(
    length: Int,
    random: SecureRandom = SecureRandom(),
): String {
    val characters = "0123456789abcdefghijklmnopqrstuvwxyz"
    return buildString(length) {
        repeat(length) { append(characters[random.nextInt(characters.length)]) }
    }
}

private fun toBcd(value: Int): Int = ((value / 10) shl 4) or (value % 10)

private fun md5Hex(bytes: ByteArray): String = MessageDigest.getInstance("MD5")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }
