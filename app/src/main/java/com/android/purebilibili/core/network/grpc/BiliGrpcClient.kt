package com.android.purebilibili.core.network.grpc

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.UUID

internal object BiliGrpcClient {
    private const val APP_BASE_URL = "https://app.bilibili.com"
    private const val USER_AGENT =
        "Mozilla/5.0 BiliDroid/2.0.1 (bbcallen@gmail.com) os/android model/android_hd mobi_app/android_hd build/2001100 channel/master innerVer/2001100 osVer/15 network/2"
    private const val TRACE_ID = "11111111111111111111111111111111:1111111111111111:0:0"
    private val grpcMediaType = "application/grpc".toMediaType()

    fun request(path: String, message: ByteArray): ByteArray {
        val url = "$APP_BASE_URL$path"
        val request = Request.Builder()
            .url(url)
            .post(ProtoWire.frame(message).toRequestBody(grpcMediaType))
            .header("content-type", "application/grpc")
            .header("grpc-encoding", "gzip")
            .header("gzip-accept-encoding", "gzip,identity")
            .header("user-agent", USER_AGENT)
            .header("x-bili-trace-id", TRACE_ID)
            .header("buvid", resolveBuvid())
            .header("cookie", buildCookieHeader())
            .apply {
                TokenManager.accessTokenCache
                    ?.takeIf { it.isNotBlank() }
                    ?.let { header("authorization", "identify_v1 $it") }
            }
            .build()

        NetworkModule.okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.bytes() ?: ByteArray(0)
            val grpcStatus = response.header("grpc-status")
            if (!response.isSuccessful || grpcStatus != "0") {
                Logger.w(
                    "BiliGrpc",
                    "gRPC request failed: path=$path http=${response.code} grpc=$grpcStatus message=${response.header("grpc-message").orEmpty()}"
                )
                error("gRPC request failed: http=${response.code}, grpc=$grpcStatus")
            }
            return ProtoWire.unframe(body)
        }
    }

    internal fun buildCookieHeader(): String {
        val cookies = mutableListOf("buvid3=${resolveBuvid()}")
        TokenManager.sessDataCache?.takeIf { it.isNotBlank() }?.let {
            cookies += "SESSDATA=$it"
        }
        TokenManager.csrfCache?.takeIf { it.isNotBlank() }?.let {
            cookies += "bili_jct=$it"
        }
        TokenManager.midCache?.takeIf { it > 0L }?.let {
            cookies += "DedeUserID=$it"
        }
        return cookies.joinToString("; ")
    }

    internal fun resolveBuvid(): String {
        val cached = TokenManager.buvid3Cache
        if (!cached.isNullOrBlank()) return cached
        val generated = UUID.randomUUID().toString().replace("-", "") + "infoc"
        TokenManager.buvid3Cache = generated
        return generated
    }

    internal fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }
}
