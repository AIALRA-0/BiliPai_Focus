package com.android.purebilibili.feature.nearby

import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import javax.net.ssl.SSLContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class NearbyTlsKeyManagerTest {
    private val key = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair().private
    // 此用例只检查握手的身份选择，不用测试链模拟证书验证或完整 TLS 连接。
    private val manager = NearbyTlsKeyManager("device", key, emptyArray())

    @Test fun engineHandshakeSelectsTheDeviceForBothRoles() {
        val engine = SSLContext.getDefault().createSSLEngine()
        assertEquals("device", manager.chooseEngineServerAlias("EC", null, engine))
        assertEquals("device", manager.chooseEngineClientAlias(arrayOf("RSA", "EC"), null, engine))
        assertSame(key, manager.getPrivateKey(manager.chooseEngineServerAlias("EC", null, engine)))
        assertSame(key, manager.getPrivateKey(manager.chooseEngineClientAlias(arrayOf("EC"), null, engine)))
    }

    @Test fun socketAndEnginePathsSelectTheSameIdentity() {
        assertEquals("device", manager.chooseServerAlias("EC", null, null))
        assertEquals(manager.chooseServerAlias("EC", null, null), manager.chooseEngineServerAlias("EC", null, null))
        assertEquals("device", manager.chooseClientAlias(arrayOf("EC"), null, null))
        assertEquals(manager.chooseClientAlias(arrayOf("EC"), null, null), manager.chooseEngineClientAlias(arrayOf("EC"), null, null))
    }

    @Test fun unsupportedAlgorithmsAndUnknownAliasesAreRejected() {
        assertNull(manager.chooseEngineServerAlias("RSA", null, null))
        assertNull(manager.chooseEngineClientAlias(arrayOf("RSA"), null, null))
        assertNull(manager.chooseEngineClientAlias(null, null, null))
        assertNull(manager.getPrivateKey("other-device"))
        assertNull(manager.getCertificateChain("other-device"))
    }
}
