package com.android.purebilibili.core.network.policy

private const val APP_FEED_HOST = "app.bilibili.com"
private const val APP_FEED_PATH = "/x/v2/feed/index"

/**
 * 合并模式 App 半边取流时携带的 `mobi_app`。
 *
 * 该值同时是 [com.android.purebilibili.core.network.ApiClient] 网络层判定「App 半边」的标记，
 * 也决定了 HD 身份头是否注入。
 */
const val MERGED_APP_FEED_MOBI_APP = "android_hd"

/**
 * 合并模式 App 半边是否应当剥离 Cookie。
 *
 * PiliNara 原版在 `AccountManager.onRequest` 中对 app 端点（`HttpString.appBaseUrl`）刻意跳过 cookie 注入
 * （源码注释：`// app端不需要管理cookie`），其身份完全由请求头承担：
 * `app-key: android_hd`、`buvid`、`fp_local`/`fp_remote`、`session_id`、`User-Agent`、
 * `x-bili-trace-id`、`x-bili-aurora-eid`/`x-bili-aurora-zone`，并配合 `access_key`（已登录时）与
 * `appkey`/`ts`/`sign` 签名。
 *
 * 若此处继续注入 SESSDATA 等登录 Cookie，App 半边的推荐结果与风控行为都会偏离原版，
 * 因此合并模式需要显式剥离。
 *
 * 判定条件收紧到「`app.bilibili.com` + `/x/v2/feed/index` + `mobi_app=android_hd`」，以保证：
 * - 不影响 Web 半边（`api.bilibili.com`，仍然携带 Cookie）；
 * - 不影响 App 单独模式（同一路径但 `mobi_app=android`）；
 * - 不影响其他任何 app 端点。
 */
fun shouldStripMergedAppFeedCookies(
    host: String,
    encodedPath: String,
    mobiApp: String?
): Boolean {
    return host == APP_FEED_HOST &&
        encodedPath == APP_FEED_PATH &&
        mobiApp == MERGED_APP_FEED_MOBI_APP
}