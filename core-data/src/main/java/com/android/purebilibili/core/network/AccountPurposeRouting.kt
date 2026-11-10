package com.android.purebilibili.core.network

import com.android.purebilibili.core.store.AccountPurpose
import com.android.purebilibili.core.store.AccountSessionStore
import com.android.purebilibili.core.store.StoredAccountSession
import okhttp3.FormBody
import okhttp3.Request

private val recommendationPaths = setOf(
    "/x/web-interface/wbi/index/top/feed/rcmd", "/x/web-interface/index/top/rcmd",
    "/x/v2/feed/index", "/x/feed/dislike", "/x/feed/dislike/cancel",
    "/x/web-interface/archive/related", "/x/web-interface/popular",
    "/x/web-interface/popular/series/one", "/x/web-interface/popular/series/list",
    "/x/web-interface/popular/precious", "/x/web-interface/ranking/v2",
    "/x/web-interface/search/type", "/x/web-interface/wbi/search/type",
    "/x/web-interface/search/all/v2", "/x/web-interface/wbi/search/all/v2",
    "/x/web-interface/search/default", "/x/web-interface/wbi/search/default",
    "/x/web-interface/search/square", "/x/web-interface/wbi/search/square", "/x/web-interface/search/suggest",
    "/x/v2/search/trending", "/x/v2/search/recommend",
)
private val historyReportPaths = setOf("/x/click-interface/web/heartbeat", "/x/v2/history/report")

internal fun resolveRequestAccountPurpose(host: String, path: String, method: String): AccountPurpose? = when {
    host !in setOf("api.bilibili.com", "app.bilibili.com") -> null
    method == "GET" && path in recommendationPaths -> AccountPurpose.RECOMMENDATION
    method == "POST" && host == "api.bilibili.com" && path in historyReportPaths -> AccountPurpose.WATCH_HISTORY
    else -> null
}

fun purposeAccount(purpose: AccountPurpose): StoredAccountSession? {
    val context = NetworkModule.appContext ?: return null
    if (AccountSessionStore.getPurposeAccountMid(context, purpose) == null) return null
    return AccountSessionStore.getPurposeAccount(context, purpose)
        ?: throw java.io.IOException("指定用途账号不可用，请重新登录或改为跟随主账号")
}

/** 在签名及调用方显式绑定之后固定会话；仅记录上报重写其同账号 CSRF 和用户字段。 */
internal fun bindRequestAccountPurpose(
    request: Request,
    resolveAccount: (AccountPurpose) -> StoredAccountSession? = ::purposeAccount,
): Request {
    if (request.tag(AccountCookieSession::class.java) != null ||
        request.header("Cookie") != null || request.header(FORCE_COOKIE_HEADER) != null) return request
    val purpose = resolveRequestAccountPurpose(request.url.host, request.url.encodedPath, request.method) ?: return request
    val account = resolveAccount(purpose) ?: return request
    val session = AccountCookieSession.from(account)
    val builder = request.newBuilder()
        .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
        .tag(AccountCookieSession::class.java, session)
    if (purpose == AccountPurpose.WATCH_HISTORY) {
        val body = request.body as? FormBody ?: throw java.io.IOException("观看上报请求格式不支持账号分配")
        val csrf = AccountCookieStore.read(NetworkModule.appContext, session)["bili_jct"] ?: account.csrf
        val rebound = FormBody.Builder()
        for (index in 0 until body.size) {
            val key = body.name(index)
            val value = when (key) {
                "csrf", "csrf_token" -> csrf
                "mid" -> account.mid.toString()
                else -> body.value(index)
            }
            rebound.add(key, value)
        }
        builder.method(request.method, rebound.build())
    }
    return builder.build()
}
