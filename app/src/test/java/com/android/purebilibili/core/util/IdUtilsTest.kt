package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class IdUtilsTest {

    @Test
    fun av2bv_convertsKnownAidsCorrectly() {
        assertEquals("BV17x411w7KC", IdUtils.av2bv(170001L))
        assertEquals("BV1xx411c7m9", IdUtils.av2bv(2L))
        assertEquals("BV17f4y1R7YS", IdUtils.av2bv(286347735L))
    }

    @Test
    fun bv2av_convertsKnownBvidsCorrectly() {
        assertEquals(170001L, IdUtils.bv2av("BV17x411w7KC"))
        assertEquals(2L, IdUtils.bv2av("BV1xx411c7m9"))
        assertEquals(286347735L, IdUtils.bv2av("BV17f4y1R7YS"))
    }

    @Test
    fun roundTrip_preservesValues() {
        val testAids = listOf(1L, 2L, 170001L, 286347735L, 88888888L, 1000000000L)
        for (aid in testAids) {
            val bvid = IdUtils.av2bv(aid)
            val convertedAid = IdUtils.bv2av(bvid)
            assertEquals(aid, convertedAid, "Round trip failed for aid=$aid (bvid=$bvid)")
        }
    }

    @Test
    fun invalidInputs_returnZeroOrEmpty() {
        assertEquals("", IdUtils.av2bv(0L))
        assertEquals("", IdUtils.av2bv(-100L))
        assertEquals(0L, IdUtils.bv2av(""))
        assertEquals(0L, IdUtils.bv2av("invalid"))
        assertEquals(0L, IdUtils.bv2av("BV1short"))
    }
}
