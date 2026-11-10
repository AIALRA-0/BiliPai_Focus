package com.android.purebilibili.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

internal enum class ProfileAccountField(val path: String, val parameter: String) {
    NAME("uname", "uname"), SEX("sex", "sex"), BIRTHDAY("birthday", "birthday")
}

internal object ProfileAccountRepository {
    private data class Credentials(val cookie: String, val csrf: String, val token: String, val platform: String)

    private fun credentials(identity: AccountSessionIdentity): Credentials = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value) { "账号已切换，请重新操作" }
        val mid = requireNotNull(identity.mid?.takeIf { it > 0 }) { "请先登录" }
        check(TokenManager.midCache == mid) { "账号正在切换，请稍后重试" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "登录已失效，请重新登录" }
        val csrf = TokenManager.csrfCache.orEmpty()
        Credentials("SESSDATA=$session; bili_jct=$csrf; DedeUserID=$mid", csrf, TokenManager.accessTokenCache.orEmpty(), TokenManager.accessTokenPlatformCache)
    }

    suspend fun updateSign(identity: AccountSessionIdentity, sign: String) {
        val auth = credentials(identity)
        require(auth.csrf.isNotBlank()) { "登录已失效，请重新登录" }
        val response = NetworkModule.api.updateMemberSign(sign, auth.csrf, auth.cookie)
        check(response.code == 0) { response.message.ifBlank { "签名保存失败（${response.code}）" } }
    }

    suspend fun update(identity: AccountSessionIdentity, field: ProfileAccountField, value: String) {
        val auth = credentials(identity)
        require(auth.token.isNotBlank()) { "请使用移动端授权登录后修改账号资料" }
        val hd = auth.platform == TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD
        require(hd || auth.platform == TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID) { "请使用移动端授权登录后修改账号资料" }
        val params = mapOf(
            "access_key" to auth.token,
            "build" to if (hd) "2001100" else "8430300",
            "c_locale" to "zh_CN", "channel" to "master",
            "mobi_app" to if (hd) "android_hd" else "android",
            "platform" to "android", "s_locale" to "zh_CN",
            "statistics" to if (hd) "{\"appId\":5,\"platform\":3,\"version\":\"2.0.1\",\"abtest\":\"\"}" else "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
            "ts" to AppSignUtils.getTimestamp().toString(), field.parameter to value
        )
        val signed = if (hd) AppSignUtils.signForAndroidHdLogin(params) else AppSignUtils.signForAndroidEncodedApi(params)
        val userAgent = if (hd) "Mozilla/5.0 BiliDroid/2.0.1 (bbcallen@gmail.com) os/android model/android_hd mobi_app/android_hd build/2001100 channel/master innerVer/2001100 osVer/15 network/2"
            else "Mozilla/5.0 BiliDroid/8.43.0 (bbcallen@gmail.com) os/android model/android mobi_app/android build/8430300 channel/master innerVer/8430300 osVer/15 network/2"
        val response = NetworkModule.api.updateMemberAppProfile(field.path, signed, auth.cookie, userAgent)
        check(response.code == 0) { response.message.ifBlank { "修改失败（${response.code}）" } }
    }

    suspend fun uploadAvatar(context: Context, identity: AccountSessionIdentity, uri: Uri) {
        val auth = credentials(identity)
        require(auth.csrf.isNotBlank()) { "登录已失效，请重新登录" }
        val bytes = withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsInput = resolver.openInputStream(uri) ?: error("无法读取图片")
            boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
            val image = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("无法读取图片")
            try {
                val side = minOf(image.width, image.height)
                val square = Bitmap.createBitmap(image, (image.width - side) / 2, (image.height - side) / 2, side, side)
                try {
                    val scaled = Bitmap.createScaledBitmap(square, minOf(side, 512), minOf(side, 512), true)
                    try { ByteArrayOutputStream().use { out -> check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)); out.toByteArray() } }
                    finally { if (scaled !== square) scaled.recycle() }
                } finally { if (square !== image) square.recycle() }
            } finally { image.recycle() }
        }
        val face = MultipartBody.Part.createFormData("face", "avatar.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        val response = NetworkModule.api.updateMemberAvatar(auth.csrf, "save".toRequestBody(), "10000".toRequestBody(), face, auth.cookie)
        check(response.code == 0) { response.message.ifBlank { "头像上传失败（${response.code}）" } }
    }
}
