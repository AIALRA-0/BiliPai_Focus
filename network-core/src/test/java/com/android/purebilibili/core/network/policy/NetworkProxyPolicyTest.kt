package com.android.purebilibili.core.network.policy

import java.net.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkProxyPolicyTest {

    private val proxy = Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 7890))
    private val globalSettings = AppHttpProxySettings(
        enabled = true,
        host = "127.0.0.1",
        portText = "7890",
        routeMode = ProxyRouteMode.GLOBAL,
    )
    private val splitSettings = AppHttpProxySettings(
        enabled = true,
        host = "127.0.0.1",
        portText = "7890",
        routeMode = ProxyRouteMode.SPLIT,
        proxiedDomains = listOf("aicu.cc"),
    )

    @Test
    fun `global mode routes every host through the proxy`() {
        val proxies = selectAppHttpProxies(globalSettings, emptyList(), host = "api.bilibili.com")
        assertEquals(listOf(proxy), proxies)
    }

    @Test
    fun `split mode routes matching host through proxy with direct fallback`() {
        val proxies = selectAppHttpProxies(splitSettings, emptyList(), host = "www.aicu.cc")
        assertEquals(listOf(proxy, Proxy.NO_PROXY), proxies)
    }

    @Test
    fun `split mode leaves unmatched host direct`() {
        val proxies = selectAppHttpProxies(splitSettings, emptyList(), host = "api.bilibili.com")
        assertEquals(listOf(Proxy.NO_PROXY), proxies)
    }

    @Test
    fun `split mode falls back to system proxies for unmatched host`() {
        val system = listOf(Proxy.NO_PROXY)
        val proxies = selectAppHttpProxies(splitSettings, system, host = "api.bilibili.com")
        assertEquals(system, proxies)
    }

    @Test
    fun `disabled proxy falls back to system list then no proxy`() {
        val settings = globalSettings.copy(enabled = false)
        assertEquals(listOf(Proxy.NO_PROXY), selectAppHttpProxies(settings, emptyList(), host = "aicu.cc"))
        val system = listOf(Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("10.0.0.1", 8080)))
        assertEquals(system, selectAppHttpProxies(settings, system, host = "aicu.cc"))
    }

    @Test
    fun `empty rules never match`() {
        assertFalse(matchesProxyDomain(emptyList(), "aicu.cc"))
        assertFalse(matchesProxyDomain(listOf("aicu.cc"), null))
        assertFalse(matchesProxyDomain(listOf("aicu.cc"), ""))
    }

    @Test
    fun `domain rules match subdomains but not lookalike suffixes`() {
        assertTrue(matchesProxyDomain(listOf("aicu.cc"), "aicu.cc"))
        assertTrue(matchesProxyDomain(listOf("aicu.cc"), "www.aicu.cc"))
        assertTrue(matchesProxyDomain(listOf("AICU.CC"), "WWW.aicu.CC"))
        assertFalse(matchesProxyDomain(listOf("aicu.cc"), "notaicu.cc"))
        assertFalse(matchesProxyDomain(listOf("aicu.cc"), "aicu.cc.evil.com"))
    }

    @Test
    fun `domain rules tolerate protocol path port and wildcard noise`() {
        assertTrue(matchesProxyDomain(listOf("https://www.aicu.cc/feed"), "aicu.cc"))
        assertTrue(matchesProxyDomain(listOf("http://aicu.cc:8080"), "aicu.cc"))
        assertTrue(matchesProxyDomain(listOf("*.aicu.cc"), "www.aicu.cc"))
        assertTrue(matchesProxyDomain(listOf(" aicu.cc "), "aicu.cc"))
    }

    @Test
    fun `parse rules dedupes and strips noise`() {
        assertEquals(
            listOf("aicu.cc", "example.com"),
            parseProxyDomainRules("https://aicu.cc/feed\n aicu.cc \nhttp://example.com:8080\n\n")
        )
    }

    @Test
    fun `global proxy keeps legacy single proxy list without direct fallback`() {
        // 全局模式下代理失败不回落直连：保持旧行为，让错误显式暴露给调用方。
        val proxies = selectAppHttpProxies(globalSettings, listOf(Proxy.NO_PROXY), host = "aicu.cc")
        assertEquals(listOf(proxy), proxies)
    }
}
