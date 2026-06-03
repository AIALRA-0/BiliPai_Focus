package com.android.purebilibili.feature.web

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WebViewNavigationPolicyTest {

    @Test
    fun resolveWebViewNavigationAction_blocksAutoAppVideoDeepLink() {
        val action = resolveWebViewNavigationAction(
            urlString = "bilibili://video/170001",
            hasUserGesture = false
        )

        assertIs<WebViewNavigationAction.Block>(action)
    }

    @Test
    fun resolveWebViewNavigationAction_convertsGestureVideoDeepLinkToWebUrl() {
        val action = resolveWebViewNavigationAction(
            urlString = "bilibili://video/170001",
            hasUserGesture = true
        )

        val loadAction = assertIs<WebViewNavigationAction.LoadInWebView>(action)
        assertEquals("https://m.bilibili.com/video/av170001", loadAction.url)
    }

    @Test
    fun resolveWebViewNavigationAction_dispatchesGestureVideoDeepLinkWithDynamicId() {
        val action = resolveWebViewNavigationAction(
            urlString = "bilibili://video/1199344045210468386",
            hasUserGesture = true
        )

        val dispatchAction = assertIs<WebViewNavigationAction.DispatchTarget>(action)
        val target = assertIs<BilibiliNavigationTarget.Dynamic>(dispatchAction.target)
        assertEquals("1199344045210468386", target.dynamicId)
    }

    @Test
    fun resolveWebViewNavigationAction_dispatchesDynamicWebLinks() {
        val action = resolveWebViewNavigationAction(
            urlString = "https://t.bilibili.com/1199344045210468386",
            hasUserGesture = true
        )

        val dispatchAction = assertIs<WebViewNavigationAction.DispatchTarget>(action)
        val target = assertIs<BilibiliNavigationTarget.Dynamic>(dispatchAction.target)
        assertEquals("1199344045210468386", target.dynamicId)
    }

    @Test
    fun resolveWebViewNavigationAction_dispatchesMobileOpusLinks() {
        val action = resolveWebViewNavigationAction(
            urlString = "https://m.bilibili.com/opus/1199344045210468386",
            hasUserGesture = true
        )

        val dispatchAction = assertIs<WebViewNavigationAction.DispatchTarget>(action)
        val target = assertIs<BilibiliNavigationTarget.Dynamic>(dispatchAction.target)
        assertEquals("1199344045210468386", target.dynamicId)
    }
}
