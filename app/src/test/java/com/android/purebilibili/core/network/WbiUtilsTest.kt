package com.android.purebilibili.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WbiUtilsTest {

    @Test
    fun `sign keeps playback params lean for PiliPlus parity`() {
        val signed = WbiUtils.sign(
            params = mapOf(
                "bvid" to "BV1TEST12345",
                "cid" to "9527",
                "qn" to "80",
                "fnval" to "4048"
            ),
            imgKey = "abcdefghijklmnopqrstuvwxyz123456",
            subKey = "123456abcdefghijklmnopqrstuvwxyz"
        )

        assertEquals("BV1TEST12345", signed["bvid"])
        assertEquals("9527", signed["cid"])
        assertEquals("80", signed["qn"])
        assertTrue(signed.containsKey("wts"))
        assertTrue(signed.containsKey("w_rid"))
        assertFalse(signed.containsKey("dm_img_list"))
        assertFalse(signed.containsKey("dm_img_str"))
        assertFalse(signed.containsKey("dm_cover_img_str"))
        assertFalse(signed.containsKey("dm_img_inter"))
    }
}
