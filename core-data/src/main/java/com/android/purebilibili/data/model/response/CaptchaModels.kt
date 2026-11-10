package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== 极验验证相关 ==========

@Serializable
data class CaptchaResponse(
    val code: Int = 0,
    val message: String = "",
    val data: CaptchaData? = null
)

@Serializable
data class CaptchaData(
    val token: String = "",
    val geetest: GeetestData? = null,
    val tencent: TencentData? = null,
    val type: String = ""  // geetest 或 tencent
)

@Serializable
data class GeetestData(
    val gt: String = "",
    val challenge: String = ""
)

@Serializable
data class TencentData(
    val appid: String = ""
)

// ========== 国际冠字码（短信登录） ==========
// docs: passport.bilibili.com/web/generic/country/list
// cid 使用条目 id；country_id 为拨号区号字符串。

@Serializable
data class PassportCountryListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PassportCountryListData? = null,
)

@Serializable
data class PassportCountryListData(
    val common: List<PassportCountryItem> = emptyList(),
    val others: List<PassportCountryItem> = emptyList(),
)

@Serializable
data class PassportCountryItem(
    val id: Int = 0,
    val cname: String = "",
    @SerialName("country_id")
    val countryId: String = "",
)

// ========== 短信验证码相关 ==========

@Serializable
data class SmsCodeResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SmsCodeData? = null
)

@Serializable
data class SmsCodeData(
    @SerialName("captcha_key")
    val captchaKey: String = "",  // 验证码登录时需要的 key
    /**
     * When a submitted Geetest result has expired, Passport returns a new
     * challenge here. The client must complete that challenge before retrying.
     */
    @SerialName("recaptcha_url")
    val recaptchaUrl: String = "",
)

// ========== RSA 密钥相关 ==========

@Serializable
data class WebKeyResponse(
    val code: Int = 0,
    val message: String = "",
    val data: WebKeyData? = null
)

@Serializable
data class WebKeyData(
    val hash: String = "",  // 用于密码加密的 salt
    val key: String = ""    // RSA 公钥
)

// ========== 登录响应 ==========

@Serializable
data class LoginResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LoginData? = null
)

@Serializable
data class LoginData(
    val status: Int = 0,       // 0=成功, 其他=需要额外验证
    val message: String = "",
    val url: String = "",      // 可能需要跳转的验证 URL
    @SerialName("refresh_token")
    val refreshToken: String = "",
    @SerialName("timestamp")
    val timestamp: Long = 0,
    @SerialName("cookie_info")
    val cookieInfo: CookieInfo? = null,
    @SerialName("token_info")
    val tokenInfo: LoginTokenInfo? = null
)

@Serializable
data class LoginTokenInfo(
    @SerialName("access_token")
    val accessToken: String = "",
    @SerialName("refresh_token")
    val refreshToken: String = ""
)

@Serializable
data class CookieInfo(
    val cookies: List<CookieItem>? = null,
    val domains: List<String>? = null
)

@Serializable
data class CookieItem(
    val name: String = "",
    val value: String = "",
    @SerialName("http_only")
    val httpOnly: Int = 0,
    val expires: Long = 0
)

// ========== 密码登录风控（安全中心） ==========

@Serializable
data class SafeCenterInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SafeCenterInfoData? = null,
)

@Serializable
data class SafeCenterInfoData(
    @SerialName("account_info")
    val accountInfo: SafeCenterAccountInfo? = null,
)

@Serializable
data class SafeCenterAccountInfo(
    @SerialName("hide_tel")
    val hideTel: String = "",
    @SerialName("hide_mail")
    val hideMail: String = "",
    @SerialName("bind_tel")
    val bindTel: Boolean = false,
    @SerialName("tel_verify")
    val telVerify: Boolean = false,
)

@Serializable
data class SafeCenterPreCaptureResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SafeCenterPreCaptureData? = null,
)

@Serializable
data class SafeCenterPreCaptureData(
    @SerialName("recaptcha_type")
    val recaptchaType: String = "",
    @SerialName("recaptcha_token")
    val recaptchaToken: String = "",
    @SerialName("gee_challenge")
    val geeChallenge: String = "",
    @SerialName("gee_gt")
    val geeGt: String = "",
)

@Serializable
data class SafeCenterSmsSendResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SafeCenterSmsSendData? = null,
)

@Serializable
data class SafeCenterSmsSendData(
    @SerialName("captcha_key")
    val captchaKey: String = "",
)

@Serializable
data class SafeCenterSmsVerifyResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SafeCenterSmsVerifyData? = null,
)

@Serializable
data class SafeCenterSmsVerifyData(
    /** Exchange code used by oauth2/access_token */
    val code: String = "",
)
