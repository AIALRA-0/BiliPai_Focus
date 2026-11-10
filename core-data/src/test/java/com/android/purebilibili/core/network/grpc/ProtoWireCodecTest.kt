package com.android.purebilibili.core.network.grpc

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ProtoWireCodecTest {
    @Test fun keepsExplicitDefaultsAndSignedIntegerWireEncoding() {
        assertContentEquals(byteArrayOf(8, 0), ProtoWire.int32(1, 0))
        assertContentEquals(byteArrayOf(16, 0), ProtoWire.bool(2, false))
        assertContentEquals(byteArrayOf(8) + ByteArray(9) { 0xFF.toByte() } + byteArrayOf(1), ProtoWire.int32(1, -1))
        val fields = ProtoWire.parseFields(byteArrayOf(8, 0x96.toByte(), 1, 21, 1, 2, 3, 4))
        assertEquals(150L, fields[0].varint)
        assertEquals(0x04030201, fields[1].fixed32)
    }

    @Test fun skipsAnUnknownGroupWithoutTreatingItsFieldsAsTopLevel() {
        val fields = ProtoWire.parseFields(byteArrayOf(11, 16, 7, 12, 24, 9))
        assertEquals(listOf(3), fields.map { it.number })
        assertEquals(9L, fields.single().varint)
    }

    @Test fun rejectsTruncatedFieldsInvalidTagsAndOverlongVarints() {
        assertFails { ProtoWire.parseFields(byteArrayOf(10, 5, 1)) }
        assertFails { ProtoWire.parseFields(byteArrayOf(0)) }
        assertFails { ProtoWire.parseFields(byteArrayOf(8) + ByteArray(11) { 0x80.toByte() }) }
    }

    @Test fun preservesCesu8EmojiCompatibilityForServerStrings() {
        val emoji = byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0xBD.toByte(), 0xED.toByte(), 0xB8.toByte(), 0x80.toByte())
        val field = ProtoWire.parseFields(byteArrayOf(10, 6) + emoji).single()
        assertEquals("😀", ProtoWire.stringValue(field))
    }
}
