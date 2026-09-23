package com.android.purebilibili.feature.livesite.huya

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

internal object HuyaAnticode {

    fun build(
        streamName: String,
        presenterUid: Long,
        antiCode: String,
        nowMillis: Long,
        random: Double
    ): String {
        val map = parseQuery(antiCode)
        if (!map.containsKey("fm")) return antiCode

        val ctype = map["ctype"] ?: "huya_pc_exe"
        val platformId = map["t"]?.toIntOrNull() ?: 0
        val isWap = platformId == 103

        val seqId = presenterUid + nowMillis
        val secretHash = md5Hex("$seqId|$ctype|$platformId")
        val convertUid = rotl64(presenterUid)
        val calcUid = if (isWap) presenterUid else convertUid
        val fm = map["fm"].orEmpty()
        val secretPrefix = String(Base64.getDecoder().decode(fm)).split("_").first()
        val wsTime = map["wsTime"].orEmpty()
        val secretStr = "${secretPrefix}_${calcUid}_${streamName}_${secretHash}_${wsTime}"
        val wsSecret = md5Hex(secretStr)

        val wsTimeLong = wsTime.toLong(16)
        val ct = (wsTimeLong + (random * 1000).toLong()) * 1000
        val uuid = (((ct % 10_000_000_000L) + (random * 1000).toLong()) * 1000 % 0xffffffffL).toString()

        val parts = LinkedHashMap<String, String>()
        parts["wsSecret"] = wsSecret
        parts["wsTime"] = wsTime
        parts["seqid"] = seqId.toString()
        parts["ctype"] = ctype
        parts["ver"] = "1"
        parts["fs"] = map["fs"].orEmpty()
        parts["fm"] = URLEncoder.encode(map["fm"].orEmpty(), "UTF-8")
        parts["t"] = platformId.toString()
        if (isWap) {
            parts["uid"] = presenterUid.toString()
            parts["uuid"] = uuid
        } else {
            parts["u"] = convertUid.toString()
        }
        return parts.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    private fun rotl64(t: Long): Long {
        val low = t and 0xffffffffL
        return ((low shl 8) or (low ushr 24)) and 0xffffffffL
    }

    internal fun parseQuery(query: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        if (query.isBlank()) return map
        val body = query.substringAfter('?')
        body.split('&').forEach { pair ->
            if (pair.isBlank()) return@forEach
            val eq = pair.indexOf('=')
            if (eq < 0) return@forEach
            val k = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
            val v = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
            if (k.isNotBlank()) map[k] = v
        }
        return map
    }

    private fun md5Hex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0f])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
