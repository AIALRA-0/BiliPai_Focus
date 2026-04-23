package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.LiveRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveAreaRoomsPolicyTest {

    @Test
    fun `risk control and rate limit errors should trigger live area fallback`() {
        assertTrue(shouldFallbackLiveAreaRooms(code = -352, message = ""))
        assertTrue(shouldFallbackLiveAreaRooms(code = -412, message = ""))
        assertTrue(shouldFallbackLiveAreaRooms(code = -509, message = ""))
        assertTrue(shouldFallbackLiveAreaRooms(code = 22015, message = ""))
        assertFalse(shouldFallbackLiveAreaRooms(code = -404, message = ""))
    }

    @Test
    fun `fallback room filter prefers exact area name matches`() {
        val rooms = listOf(
            LiveRoom(roomid = 1, areaName = "英雄联盟", title = "lol"),
            LiveRoom(roomid = 2, areaName = "无畏契约", title = "valorant"),
            LiveRoom(roomid = 3, areaName = "无畏契约", title = "rank")
        )

        val filtered = filterFallbackLiveAreaRooms(rooms, areaTitle = "无畏契约")

        assertEquals(listOf(2L, 3L), filtered.map { it.roomid })
    }

    @Test
    fun `fallback room filter can fall back to title matches`() {
        val rooms = listOf(
            LiveRoom(roomid = 1, areaName = "网游", title = "无畏契约排位"),
            LiveRoom(roomid = 2, areaName = "网游", title = "英雄联盟")
        )

        val filtered = filterFallbackLiveAreaRooms(rooms, areaTitle = "无畏契约")

        assertEquals(listOf(1L), filtered.map { it.roomid })
    }

    @Test
    fun `live area error message should hide raw risk code`() {
        assertEquals(
            "触发风控，请稍后重试",
            resolveLiveAreaRoomsErrorMessage(code = -352, message = "")
        )
    }
}
