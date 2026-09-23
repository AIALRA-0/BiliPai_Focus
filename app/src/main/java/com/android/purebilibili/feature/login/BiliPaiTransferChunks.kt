package com.android.purebilibili.feature.login

import android.util.Base64

data class BiliPaiTransferChunk(val transferId: String, val index: Int, val total: Int, val data: String)

object BiliPaiTransferChunks {
    private const val PREFIX = "bilipai://transfer/chunk?"
    private const val MAX_CHUNK_DATA = 1_500

    fun split(transferId: String, envelope: String): List<String> {
        val encoded = Base64.encodeToString(envelope.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        val total = (encoded.length + MAX_CHUNK_DATA - 1) / MAX_CHUNK_DATA
        require(total in 1..128) { "加密会话过大，无法分片" }
        return (0 until total).map { index ->
            val start = index * MAX_CHUNK_DATA
            val end = minOf(start + MAX_CHUNK_DATA, encoded.length)
            "$PREFIX" + "id=$transferId&i=$index&n=$total&d=${encoded.substring(start, end)}"
        }
    }

    fun parse(raw: String): BiliPaiTransferChunk? {
        if (!raw.startsWith(PREFIX) || raw.length > 2_400) return null
        val values = raw.substringAfter('?').split('&').mapNotNull {
            val p = it.split('=', limit = 2); if (p.size == 2) p[0] to p[1] else null
        }.toMap()
        val index = values["i"]?.toIntOrNull() ?: return null
        val total = values["n"]?.toIntOrNull() ?: return null
        if (values["id"].isNullOrBlank() || total !in 1..128 || index !in 0 until total) return null
        return BiliPaiTransferChunk(values.getValue("id"), index, total, values["d"].orEmpty())
    }

    fun join(chunks: Collection<BiliPaiTransferChunk>): String? {
        if (chunks.isEmpty()) return null
        val first = chunks.first()
        if (chunks.size != first.total || chunks.any { it.transferId != first.transferId || it.total != first.total }) return null
        val encoded = (0 until first.total).map { i -> chunks.firstOrNull { it.index == i }?.data ?: return null }.joinToString("")
        return runCatching { String(Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE)) }.getOrNull()
    }
}
