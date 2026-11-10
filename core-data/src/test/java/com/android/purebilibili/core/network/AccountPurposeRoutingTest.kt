package com.android.purebilibili.core.network

import com.android.purebilibili.core.store.AccountPurpose
import com.android.purebilibili.core.store.StoredAccountSession
import okhttp3.FormBody
import okhttp3.Request
import kotlin.test.*

class AccountPurposeRoutingTest {
    private val account = StoredAccountSession(mid = 88, sessData = "role-session", csrf = "role-csrf")

    @Test fun `routes only documented host path and method combinations`() {
        assertEquals(AccountPurpose.RECOMMENDATION, resolveRequestAccountPurpose("api.bilibili.com", "/x/web-interface/wbi/index/top/feed/rcmd", "GET"))
        assertEquals(AccountPurpose.RECOMMENDATION, resolveRequestAccountPurpose("app.bilibili.com", "/x/v2/feed/index", "GET"))
        assertEquals(AccountPurpose.WATCH_HISTORY, resolveRequestAccountPurpose("api.bilibili.com", "/x/v2/history/report", "POST"))
        listOf("/x/v2/reply/add", "/x/web-interface/archive/like", "/x/v3/fav/resource/deal", "/x/web-interface/nav", "/x/web-interface/history/cursor").forEach {
            assertNull(resolveRequestAccountPurpose("api.bilibili.com", it, "POST"))
            assertNull(resolveRequestAccountPurpose("api.bilibili.com", it, "GET"))
        }
        assertNull(resolveRequestAccountPurpose("api.bilibili.com.evil.test", "/x/v2/feed/index", "GET"))
        assertNull(resolveRequestAccountPurpose("api.bilibili.com", "/x/v2/history/report", "GET"))
    }

    @Test fun `history identity and csrf are rebound together while media fields stay intact`() {
        val request = Request.Builder().url("https://api.bilibili.com/x/click-interface/web/heartbeat")
            .post(FormBody.Builder().add("mid", "1").add("csrf", "main-csrf").add("csrf_token", "main-csrf")
                .add("bvid", "BVexample").add("played_time", "42").build()).build()
        val routed = bindRequestAccountPurpose(request) { account }
        val form = routed.body as FormBody
        val fields = (0 until form.size).associate { form.name(it) to form.value(it) }
        assertEquals("88", fields["mid"])
        assertEquals("role-csrf", fields["csrf"])
        assertEquals("role-csrf", fields["csrf_token"])
        assertEquals("BVexample", fields["bvid"])
        assertEquals("42", fields["played_time"])
        assertEquals("role-session", routed.tag(AccountCookieSession::class.java)?.sessData)
    }

    @Test fun `explicit credentials and prebound signed requests remain authoritative`() {
        val base = Request.Builder().url("https://app.bilibili.com/x/v2/feed/index").build()
        val session = AccountCookieSession.from("signed-session", "signed-csrf", 3L, "device")
        val tagged = base.newBuilder().tag(AccountCookieSession::class.java, session).build()
        assertSame(tagged, bindRequestAccountPurpose(tagged) { error("must not rebind signed requests") })
        val explicit = base.newBuilder().header("Cookie", "custom=value").build()
        assertSame(explicit, bindRequestAccountPurpose(explicit) { error("must not rebind explicit cookies") })
        assertSame(base, bindRequestAccountPurpose(base) { null })
    }
}
