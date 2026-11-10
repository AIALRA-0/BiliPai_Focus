package com.android.purebilibili.feature.nearby

import java.io.EOFException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NearbyConnectionFailureTest {
    @Test fun connectionStagesProduceDifferentTimeoutHints() {
        val error = SocketTimeoutException("private-detail")
        assertTrue(nearbyConnectionHint(error, NearbyConnectionStage.CONNECT).contains("连接地址超时"))
        assertTrue(nearbyConnectionHint(error, NearbyConnectionStage.HANDSHAKE).contains("握手超时"))
        assertTrue(nearbyConnectionHint(error, NearbyConnectionStage.CONFIRM).contains("确认超时"))
    }

    @Test fun networkAndIdentityFailuresDoNotCollapseIntoOneMessage() {
        assertTrue(nearbyConnectionHint(ConnectException(), NearbyConnectionStage.CONNECT).contains("连接被拒绝"))
        assertTrue(nearbyConnectionHint(NoRouteToHostException(), NearbyConnectionStage.CONNECT).contains("不可达"))
        assertTrue(nearbyConnectionHint(SSLHandshakeException("handshake"), NearbyConnectionStage.HANDSHAKE).contains("加密握手失败"))
        assertTrue(nearbyConnectionHint(EOFException(), NearbyConnectionStage.CONFIRM).contains("关闭了连接"))
    }

    @Test fun knownFailureSurvivesTransportWrapping() {
        val failure = NearbyConnectionFailure("两端设备接力协议不兼容，请使用相同版本")
        assertEquals(failure.hint, nearbyConnectionHint(java.io.IOException("private-detail", failure),
            NearbyConnectionStage.CONFIRM))
    }

    @Test fun rawErrorsCannotExposePairingSecrets() {
        for (error in listOf(IllegalArgumentException("private-token"), SSLHandshakeException("private-token"),
            SecurityException("private-token"), RuntimeException("private-token"))) {
            assertFalse(nearbyConnectionHint(error, NearbyConnectionStage.PREPARE).contains("private-token"))
        }
        val explicit = NearbyConnectionFailure("对端拒绝了配对")
        assertEquals(explicit.hint, nearbyConnectionHint(explicit, NearbyConnectionStage.CONFIRM))
    }
}
