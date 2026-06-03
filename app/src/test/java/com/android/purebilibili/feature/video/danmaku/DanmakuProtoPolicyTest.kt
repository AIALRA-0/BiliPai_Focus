package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DanmakuProtoPolicyTest {

    @Test
    fun parseWebViewReply_supportsNewSchemaDmSgeOnField4() {
        val payload = encodeDmWebViewReply(
            dmSgeFieldNumber = 4,
            pageSize = 360_000L,
            total = 16L,
            countFieldNumber = 8,
            count = 16_000L
        )

        val reply = DanmakuProto.parseWebViewReply(payload)

        val dmSge = assertNotNull(reply.dmSge)
        assertEquals(360_000L, dmSge.pageSize)
        assertEquals(16L, dmSge.total)
        assertEquals(16_000L, reply.count)
    }

    @Test
    fun parseWebViewReply_readsDocumentedNewSchemaWithoutFieldCollisions() {
        val dmSge = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 360_000L)
            addFieldVarint(fieldNumber = 2, value = 4L)
        }.toByteArray()
        val flag = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 1L)
            addFieldBytes(fieldNumber = 2, value = "推荐开启云屏蔽".encodeToByteArray())
            addFieldVarint(fieldNumber = 3, value = 1L)
        }.toByteArray()
        val command = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 38469676112019463L)
            addFieldVarint(fieldNumber = 2, value = 236871317L)
            addFieldVarint(fieldNumber = 3, value = 501183549L)
            addFieldBytes(fieldNumber = 4, value = "#ATTENTION#".encodeToByteArray())
            addFieldBytes(fieldNumber = 5, value = "关注按钮".encodeToByteArray())
            addFieldVarint(fieldNumber = 6, value = 157818L)
            addFieldBytes(
                fieldNumber = 9,
                value = """{"duration":6000,"posX":240,"posY":160,"icon":"https://example.com/follow.png","type":2}""".encodeToByteArray()
            )
            addFieldBytes(fieldNumber = 10, value = "38469676112019463".encodeToByteArray())
        }.toByteArray()
        val dmSetting = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 1L)
            addFieldVarint(fieldNumber = 4, value = 1L)
            addFieldVarint(fieldNumber = 5, value = 0L)
            addFieldVarint(fieldNumber = 6, value = 1L)
            addFieldVarint(fieldNumber = 7, value = 1L)
            addFieldVarint(fieldNumber = 8, value = 0L)
            addFieldFloat(fieldNumber = 11, value = 0.65f)
            addFieldVarint(fieldNumber = 12, value = 75L)
            addFieldFloat(fieldNumber = 13, value = 1.25f)
            addFieldFloat(fieldNumber = 14, value = 1.1f)
        }.toByteArray()
        val payload = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 0L)
            addFieldBytes(fieldNumber = 2, value = "text".encodeToByteArray())
            addFieldBytes(fieldNumber = 3, value = "side".encodeToByteArray())
            addFieldBytes(fieldNumber = 4, value = dmSge)
            addFieldBytes(fieldNumber = 5, value = flag)
            addFieldBytes(fieldNumber = 6, value = "https://i0.hdslb.com/bfs/dm/special.bin".encodeToByteArray())
            addFieldVarint(fieldNumber = 7, value = 0L)
            addFieldVarint(fieldNumber = 8, value = 4200L)
            addFieldBytes(fieldNumber = 9, value = command)
            addFieldBytes(fieldNumber = 10, value = dmSetting)
        }.toByteArray()

        val reply = DanmakuProto.parseWebViewReply(payload)

        assertEquals(listOf("https://i0.hdslb.com/bfs/dm/special.bin"), reply.specialDms)
        assertFalse(reply.checkBox)
        assertEquals(4200L, reply.count)
        assertEquals(1, reply.commandDms.size)
        val item = reply.commandDms.first()
        assertEquals(501183549L, item.mid)
        assertEquals("#ATTENTION#", item.command)
        assertEquals("""{"duration":6000,"posX":240,"posY":160,"icon":"https://example.com/follow.png","type":2}""", item.extra)
        val setting = assertNotNull(reply.dmSetting)
        assertEquals(true, setting.dmSwitch)
        assertEquals(true, setting.blocktop)
        assertEquals(false, setting.blockscroll)
        assertEquals(0.65f, setting.opacity)
        assertEquals(75, setting.dmarea)
        assertEquals(1.25f, setting.speedplus)
        assertEquals(1.1f, setting.fontsize)
    }

    @Test
    fun parseWebViewReply_keepsOldSchemaDmSgeOnField3() {
        val payload = encodeDmWebViewReply(
            dmSgeFieldNumber = 3,
            pageSize = 360_000L,
            total = 9L,
            countFieldNumber = 7,
            count = 9_999L
        )

        val reply = DanmakuProto.parseWebViewReply(payload)

        val dmSge = assertNotNull(reply.dmSge)
        assertEquals(360_000L, dmSge.pageSize)
        assertEquals(9L, dmSge.total)
        assertEquals(9_999L, reply.count)
    }

    @Test
    fun parseDmSegMobileReply_readsPiliPlusExtendedElemFields() {
        val elem = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = 123L)
            addFieldVarint(fieldNumber = 2, value = 4567L)
            addFieldVarint(fieldNumber = 3, value = 1L)
            addFieldVarint(fieldNumber = 4, value = 25L)
            addFieldVarint(fieldNumber = 5, value = 0xFFFFFFL)
            addFieldBytes(fieldNumber = 6, value = "hash".encodeToByteArray())
            addFieldBytes(fieldNumber = 7, value = "关注弹幕".encodeToByteArray())
            addFieldVarint(fieldNumber = 9, value = 8L)
            addFieldVarint(fieldNumber = 11, value = 0L)
            addFieldVarint(fieldNumber = 15, value = 12L)
            addFieldVarint(
                fieldNumber = 24,
                value = DanmakuProto.DmColorfulTypeVipGradualColor.toLong()
            )
            addFieldVarint(fieldNumber = 28, value = 3L)
            addFieldVarint(fieldNumber = 29, value = 1L)
        }.toByteArray()
        val payload = buildList<Byte> {
            addFieldBytes(fieldNumber = 1, value = elem)
        }.toByteArray()

        val parsed = DanmakuProto.parse(payload)

        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals(123L, item.id)
        assertEquals(4567, item.progress)
        assertEquals("关注弹幕", item.content)
        assertEquals(8, item.weight)
        assertEquals(12L, item.like)
        assertEquals(DanmakuProto.DmColorfulTypeVipGradualColor, item.colorful)
        assertEquals(3, item.count)
        assertEquals(true, item.isSelf)
    }

    private fun encodeDmWebViewReply(
        dmSgeFieldNumber: Int,
        pageSize: Long,
        total: Long,
        countFieldNumber: Int,
        count: Long
    ): ByteArray {
        val dmSge = buildList<Byte> {
            addFieldVarint(fieldNumber = 1, value = pageSize)
            addFieldVarint(fieldNumber = 2, value = total)
        }.toByteArray()

        val message = buildList<Byte> {
            addFieldBytes(fieldNumber = dmSgeFieldNumber, value = dmSge)
            addFieldVarint(fieldNumber = countFieldNumber, value = count)
        }

        return message.toByteArray()
    }

    private fun MutableList<Byte>.addFieldVarint(fieldNumber: Int, value: Long) {
        addAll(encodeVarint(((fieldNumber shl 3) or 0).toLong()))
        addAll(encodeVarint(value))
    }

    private fun MutableList<Byte>.addFieldBytes(fieldNumber: Int, value: ByteArray) {
        addAll(encodeVarint(((fieldNumber shl 3) or 2).toLong()))
        addAll(encodeVarint(value.size.toLong()))
        value.forEach { add(it) }
    }

    private fun MutableList<Byte>.addFieldFloat(fieldNumber: Int, value: Float) {
        addAll(encodeVarint(((fieldNumber shl 3) or 5).toLong()))
        val bits = java.lang.Float.floatToIntBits(value)
        add((bits and 0xFF).toByte())
        add(((bits ushr 8) and 0xFF).toByte())
        add(((bits ushr 16) and 0xFF).toByte())
        add(((bits ushr 24) and 0xFF).toByte())
    }

    private fun encodeVarint(value: Long): List<Byte> {
        var remaining = value
        val bytes = mutableListOf<Byte>()
        do {
            var next = (remaining and 0x7F).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) {
                next = next or 0x80
            }
            bytes.add(next.toByte())
        } while (remaining != 0L)
        return bytes
    }
}
