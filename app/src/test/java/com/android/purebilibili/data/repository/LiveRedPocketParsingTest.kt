package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveRedPocketParsingTest {

    @Test
    fun `red pocket parser extracts first popularity red pocket`() {
        val json = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "popularity_red_pocket": [
                  {
                    "lot_id": 622474,
                    "sender_name": "九泽糖糖",
                    "danmu": "老板大气！",
                    "h5_url": "https://live.bilibili.com/p/html/live-app-red-envelope/popularity.html",
                    "user_status": 2,
                    "total_price": 1600,
                    "end_time": 1645358284,
                    "current_time": 1645358231,
                    "awards": [
                      {"gift_name": "打call", "num": 2},
                      {"gift_name": "牛哇", "num": 3}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val info = parseLiveRedPocketInfo(json)

        assertEquals(622474L, info?.lotId)
        assertEquals("九泽糖糖", info?.senderName)
        assertEquals("老板大气！", info?.danmu)
        assertEquals("打call x2、牛哇 x3", info?.awardsText)
        assertEquals(53, info?.remainingSeconds)
    }

    @Test
    fun `red pocket parser returns null when room has no popularity red pocket`() {
        val json = """{"code":0,"data":{"popularity_red_pocket":null}}"""

        assertNull(parseLiveRedPocketInfo(json))
    }
}
