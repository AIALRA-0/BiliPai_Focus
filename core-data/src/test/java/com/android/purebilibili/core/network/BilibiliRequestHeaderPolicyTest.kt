package com.android.purebilibili.core.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BilibiliRequestHeaderPolicyTest {
    @Test
    fun `Bilibili services and media retain default headers`() {
        listOf("bilibili.com", "api.bilibili.com", "i0.hdslb.com", "upos.bilivideo.com", "upos.bilivideo.cn")
            .forEach { assertTrue(usesDefaultBilibiliHeaders(it), it) }
    }

    @Test
    fun `RSS image hosts and lookalike domains do not receive Bilibili headers`() {
        listOf("image.gcores.com", "www.jintiankansha.me", "mmbiz.qpic.cn", "evilbilibili.com", "bilibili.com.example.com")
            .forEach { assertFalse(usesDefaultBilibiliHeaders(it), it) }
    }
}
