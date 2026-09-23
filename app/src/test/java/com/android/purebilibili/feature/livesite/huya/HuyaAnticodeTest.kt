package com.android.purebilibili.feature.livesite.huya

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HuyaAnticodeTest {

    @Test
    fun passthroughWhenFmMissing() {
        val antiCode = "wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl"
        val out = HuyaAnticode.build(
            streamName = "123456-100",
            presenterUid = 777L,
            antiCode = antiCode,
            nowMillis = 1700000000000L,
            random = 0.5
        )
        assertEquals(antiCode, out)
    }

    @Test
    fun nonWapBranchEmitsUAndNotUid() {
        val antiCode = "fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl"
        val out = HuyaAnticode.build(
            streamName = "123456-100",
            presenterUid = 777L,
            antiCode = antiCode,
            nowMillis = 1700000000000L,
            random = 0.5
        )
        val map = HuyaAnticode.parseQuery(out)
        assertTrue(map.containsKey("u"))
        assertFalse(map.containsKey("uid"))
        assertFalse(map.containsKey("uuid"))
        assertEquals("100", map["t"])
        assertEquals("1", map["ver"])
        assertEquals("65e1a2b3", map["wsTime"])
        assertEquals(32, map["wsSecret"]?.length)
        assertEquals("1700000000777", map["seqid"])
    }

    @Test
    fun wapBranchEmitsUidAndUuid() {
        val antiCode = "fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_mobile&t=103&fs=fsl"
        val out = HuyaAnticode.build(
            streamName = "123456-100",
            presenterUid = 777L,
            antiCode = antiCode,
            nowMillis = 1700000000000L,
            random = 0.5
        )
        val map = HuyaAnticode.parseQuery(out)
        assertTrue(map.containsKey("uid"))
        assertTrue(map.containsKey("uuid"))
        assertFalse(map.containsKey("u"))
        assertEquals("103", map["t"])
        assertEquals("huya_mobile", map["ctype"])
    }

    @Test
    fun deterministicForFixedInputs() {
        val antiCode = "fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl"
        val a = HuyaAnticode.build("123456-100", 777L, antiCode, 1700000000000L, 0.5)
        val b = HuyaAnticode.build("123456-100", 777L, antiCode, 1700000000000L, 0.5)
        assertEquals(a, b)
    }

    @Test
    fun nonWapFullOutputMatchesJsReference() {
        val antiCode = "fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl"
        val out = HuyaAnticode.build("123456-100", 777L, antiCode, 1700000000000L, 0.5)
        assertEquals(
            "wsSecret=debfaa5db707bffcd617c460bf87c8c5&wsTime=65e1a2b3&seqid=1700000000777&ctype=huya_pc_exe&ver=1&fs=fsl&fm=RndzX3h4eA%3D%3D&t=100&u=198912",
            out
        )
    }

    @Test
    fun wapFullOutputMatchesJsReference() {
        val antiCode = "fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_mobile&t=103&fs=fsl"
        val out = HuyaAnticode.build("123456-100", 777L, antiCode, 1700000000000L, 0.5)
        assertEquals(
            "wsSecret=e03e776308d9baa46e13306ee058eeb8&wsTime=65e1a2b3&seqid=1700000000777&ctype=huya_mobile&ver=1&fs=fsl&fm=RndzX3h4eA%3D%3D&t=103&uid=777&uuid=848208210",
            out
        )
    }
}
