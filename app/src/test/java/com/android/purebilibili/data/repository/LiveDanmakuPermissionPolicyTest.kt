package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDanmakuPermissionPolicyTest {

    @Test
    fun `permission parser exposes available colors and modes`() {
        val permission = parseLiveDanmakuPermission(
            """
            {
              "code": 0,
              "data": {
                "group": [
                  {
                    "name": "普",
                    "color": [
                      {"name":"白色","color":"16777215","color_hex":"ffffff","status":1},
                      {"name":"紫色","color":"14893055","color_hex":"e33fff","status":0}
                    ]
                  }
                ],
                "mode": [
                  {"name":"滚动","mode":1,"status":1},
                  {"name":"底部","mode":4,"status":0}
                ]
              }
            }
            """.trimIndent()
        )

        assertTrue(permission.canSend)
        assertEquals("可发送弹幕", permission.statusText)
        assertEquals(listOf("白色"), permission.availableColors.map { it.name })
        assertEquals(listOf("滚动"), permission.availableModes.map { it.name })
    }

    @Test
    fun `permission parser turns api failure into disabled state`() {
        val permission = parseLiveDanmakuPermission("""{"code":-400,"message":"参数错误"}""")

        assertFalse(permission.canSend)
        assertEquals("参数错误", permission.statusText)
        assertEquals(0, permission.maxLength)
    }
}
