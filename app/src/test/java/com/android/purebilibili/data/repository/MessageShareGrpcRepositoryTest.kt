package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.grpc.ProtoWire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageShareGrpcRepositoryTest {
    @Test
    fun buildsNativeShareV2GrpcRequest() {
        val request = buildDynamicShareSendRequest(
            senderUid = 100L,
            receiverId = 200L,
            content = "{\"source\":11}",
            timestampSeconds = 300L,
            deviceId = "device-id",
        )

        val outerFields = ProtoWire.parseFields(request)
        val messageField = assertNotNull(outerFields.firstOrNull { it.number == 1 })
        assertEquals("device-id", ProtoWire.stringValue(assertNotNull(outerFields.firstOrNull { it.number == 5 })))

        val messageFields = ProtoWire.parseFields(messageField.bytes).associateBy { it.number }
        assertEquals(100L, messageFields.getValue(1).varint)
        assertEquals(1L, messageFields.getValue(2).varint)
        assertEquals(200L, messageFields.getValue(3).varint)
        assertEquals(7L, messageFields.getValue(5).varint)
        assertEquals("{\"source\":11}", ProtoWire.stringValue(messageFields.getValue(6)))
        assertEquals(300L, messageFields.getValue(8).varint)
        assertEquals(1L, messageFields.getValue(16).varint)
    }

    @Test
    fun parsesShareListUserNamesAndAvatarsFromGrpcPayload() {
        val first = ProtoWire.message(
            ProtoWire.int64(1, 471278344L),
            ProtoWire.string(2, "用户甲"),
            ProtoWire.string(3, "https://i0.hdslb.com/a.jpg"),
        )
        val second = ProtoWire.message(
            ProtoWire.int64(1, 12076317L),
            ProtoWire.string(2, "用户乙"),
            ProtoWire.string(3, "https://i0.hdslb.com/b.jpg"),
        )
        val payload = ProtoWire.message(
            ProtoWire.bytes(1, first),
            ProtoWire.bytes(1, second),
        )

        val targets = parseMessageShareTargets(payload)

        assertEquals(listOf(471278344L, 12076317L), targets.map { it.mid })
        assertEquals(listOf("用户甲", "用户乙"), targets.map { it.name })
        assertEquals("https://i0.hdslb.com/a.jpg", targets.first().avatarUrl)
    }
}
