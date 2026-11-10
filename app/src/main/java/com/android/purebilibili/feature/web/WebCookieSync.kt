package com.android.purebilibili.feature.web

import android.webkit.CookieManager
import com.android.purebilibili.core.network.AccountCookieSession
import com.android.purebilibili.core.network.AccountCookieStore
import com.android.purebilibili.core.network.NetworkModule

/**
 * 内嵌 WebView 与 B 站 H5 页面共享登录态：把主账号 Cookie 写入系统 CookieManager。
 *
 * OkHttp 侧的 CookieJar 是独立内存态，WebView 读不到；不同步时 H5 一律按未登录渲染。
 * 未登录（主账号无 Cookie）时移除历史遗留的登录 Cookie，避免 WebView 残留过期账号。
 */
internal fun syncBilibiliLoginCookies() {
    val cookies = runCatching {
        AccountCookieStore.read(
            NetworkModule.appContext,
            AccountCookieSession.main(),
        )
    }.getOrNull().orEmpty()

    val manager = CookieManager.getInstance()
    manager.setAcceptCookie(true)
    val target = "https://www.bilibili.com"
    if (cookies.isEmpty()) {
        for (name in WEB_LOGIN_COOKIE_NAMES) {
            manager.setCookie(target, "$name=; Domain=.bilibili.com; Path=/; Max-Age=0")
        }
    } else {
        for ((name, value) in cookies) {
            manager.setCookie(target, "$name=$value; Domain=.bilibili.com; Path=/")
        }
        // 上一次登录残留、本次未再下发的登录 Cookie 一并清掉。
        for (name in WEB_LOGIN_COOKIE_NAMES) {
            if (name !in cookies) {
                manager.setCookie(target, "$name=; Domain=.bilibili.com; Path=/; Max-Age=0")
            }
        }
    }
    manager.flush()
}

private val WEB_LOGIN_COOKIE_NAMES = setOf(
    "SESSDATA",
    "bili_jct",
    "DedeUserID",
    "DedeUserID__ckMd5",
    "sid",
    "ac_time_value",
    "buvid3",
    "buvid4",
)
