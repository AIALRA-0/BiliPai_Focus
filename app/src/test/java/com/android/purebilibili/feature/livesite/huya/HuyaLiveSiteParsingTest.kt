package com.android.purebilibili.feature.livesite.huya

import com.android.purebilibili.feature.livesite.LiveSiteRoomDetail
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HuyaLiveSiteParsingTest {

    private val site = HuyaLiveSite(nowMillis = { 1700000000000L }, random = { 0.5 })

    @Test
    fun parseSubCategories() {
        val body = """
            {"data":[{"gid":1,"gameFullName":"英雄联盟"},{"gid":2,"gameFullName":"王者荣耀"}]}
        """.trimIndent()
        val areas = site.parseSubCategories(body)
        assertEquals(2, areas.size)
        assertEquals("1", areas[0].id)
        assertEquals("英雄联盟", areas[0].name)
        assertTrue(areas[0].pic.contains("/1-MS.jpg"))
    }

    @Test
    fun parseRoomList() {
        val body = """
            {"data":{"datas":[{"profileRoom":"123","introduction":"标题","roomName":"备选","nick":"主播","screenshot":"http://x/cover.jpg","avatar180":"http://x/a.jpg","gameFullName":"英雄联盟","totalCount":"100"}],"page":1,"totalPage":3}}
        """.trimIndent()
        val page = site.parseRoomList(body)
        assertEquals(1, page.items.size)
        assertEquals("123", page.items[0].roomId)
        assertEquals("标题", page.items[0].title)
        assertEquals("主播", page.items[0].nick)
        assertEquals("http://x/cover.jpg?x-oss-process=style/w338_h190&", page.items[0].cover)
        assertEquals("英雄联盟", page.items[0].area)
        assertTrue(page.hasMore)
    }

    @Test
    fun parseRoomListFallsBackToRoomName() {
        val body = """
            {"data":{"datas":[{"profileRoom":"1","roomName":"只用房间名","nick":"n","screenshot":"http://x/c.jpg?x=1","totalCount":"1"}],"page":2,"totalPage":2}}
        """.trimIndent()
        val page = site.parseRoomList(body)
        assertEquals("只用房间名", page.items[0].title)
        assertEquals("http://x/c.jpg?x=1", page.items[0].cover)
        assertFalse(page.hasMore)
    }

    @Test
    fun parseSearchRoomsMatchesRoomId() {
        val body = """
            {"response":{"3":{"docs":[{"game_screenshot":"http://x/s.jpg","game_introduction":"搜索标题","uid":"100","yyid":"200","room_id":"999","game_nick":"主播","gameName":"网游","game_imgUrl":"http://x/img.jpg","game_total_count":"50"}]},"1":{"docs":[{"uid":"100","yyid":"200","room_id":"888"}]}}}
        """.trimIndent()
        val page = site.parseSearchRooms(body)
        assertEquals(1, page.items.size)
        assertEquals("888", page.items[0].roomId)
        assertEquals("搜索标题", page.items[0].title)
        assertEquals("http://x/s.jpg?x-oss-process=style/w338_h190&", page.items[0].cover)
        assertTrue(page.hasMore)
    }

    @Test
    fun parseRoomDetailExtractsLinesAndQualities() {
        val body = """
            {"status":200,"data":{"liveStatus":"ON","welcomeText":"欢迎","profileInfo":{"nick":"主播名","avatar180":"http://x/a.jpg"},"liveData":{"screenshot":"http://x/cover.jpg","userCount":"123","gameFullName":"英雄联盟","introduction":"房间标题","bitRateInfo":"[{\"sDisplayName\":\"原画\",\"iBitRate\":0},{\"sDisplayName\":\"高清\",\"iBitRate\":2000}]"},"stream":{"baseSteamInfoList":[{"sCdnType":"AL","sFlvUrl":"http://cdn.al.huya.com/src","sFlvAntiCode":"fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl","sStreamName":"123456-100","lChannelId":"777","lSubChannelId":"888"}],"flv":{"multiLine":[{"url":"http://cdn.al.huya.com/src/123456-100.flv","cdnType":"AL","sCdnType":"AL"}],"rateArray":[]}}}}
        """.trimIndent()
        val detail = site.parseRoomDetail("123", body)
        assertTrue(detail.live)
        assertEquals("房间标题", detail.title)
        assertEquals("主播名", detail.nick)
        assertEquals("http://x/cover.jpg", detail.cover)
        assertEquals("欢迎", detail.notice)
        assertEquals(2, detail.qualities.size)
        assertEquals("原画", detail.qualities[0].name)
        assertEquals("高清", detail.qualities[1].name)
        assertTrue(detail.playData.contains("cdn.al.huya.com"))
        assertTrue(detail.playData.contains("123456-100"))
    }

    @Test
    fun parseRoomDetailOfflineWhenStatusNot200() {
        val body = """{"status":404,"data":{}}"""
        val detail = site.parseRoomDetail("999", body)
        assertFalse(detail.live)
        assertTrue(detail.qualities.isEmpty())
    }

    @Test
    fun parseRoomDetailFillsDefaultQualitiesWhenEmpty() {
        val body = """
            {"status":200,"data":{"liveStatus":"ON","profileInfo":{"nick":"n"},"liveData":{"introduction":"t"},"stream":{"baseSteamInfoList":[{"sCdnType":"AL","sFlvUrl":"http://x/src","sFlvAntiCode":"fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl","sStreamName":"s","lChannelId":"1","lSubChannelId":"2"}],"flv":{"multiLine":[{"url":"http://x/src/s.flv","cdnType":"AL","sCdnType":"AL"}],"rateArray":[]}}}}
        """.trimIndent()
        val detail = site.parseRoomDetail("1", body)
        assertEquals(2, detail.qualities.size)
        assertEquals("原画", detail.qualities[0].name)
    }

    @Test
    fun getPlayUrlsBuildsFlvUrlWithAnticodeAndHeaders() {
        val playData = """
            {"lines":[{"flvUrl":"http://cdn.al.huya.com/src","streamName":"123456-100","flvAntiCode":"fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl","presenterUid":777,"cdnType":"AL"}],"bitRates":[{"bitRate":0,"name":"原画"},{"bitRate":2000,"name":"高清"}]}
        """.trimIndent()
        val detail = LiveSiteRoomDetail(
            roomId = "123",
            title = "t",
            nick = "n",
            cover = "",
            live = true,
            qualities = emptyList(),
            playData = playData
        )
        val streams = runBlocking { site.getPlayUrls(detail, "0").getOrThrow() }
        assertEquals(1, streams.size)
        val url = streams[0].url
        assertTrue(url.startsWith("http://cdn.al.huya.com/src/123456-100.flv?"))
        assertTrue(url.contains("wsSecret="))
        assertTrue(url.contains("codec=264"))
        assertFalse(url.contains("ratio="))
        assertEquals("video/x-flv", streams[0].contentType)
        assertEquals("https://www.huya.com/", streams[0].headers["Referer"])
        assertNotNull(streams[0].headers["User-Agent"])
    }

    @Test
    fun getPlayUrlsAppendsRatioForNonZeroBitRate() {
        val playData = """
            {"lines":[{"flvUrl":"http://x/src","streamName":"s","flvAntiCode":"fm=RndzX3h4eA%3D%3D&wsTime=65e1a2b3&ctype=huya_pc_exe&t=100&fs=fsl","presenterUid":1,"cdnType":"AL"}],"bitRates":[{"bitRate":2000,"name":"高清"}]}
        """.trimIndent()
        val detail = LiveSiteRoomDetail(
            roomId = "1", title = "t", nick = "n", cover = "", live = true, playData = playData
        )
        val streams = runBlocking { site.getPlayUrls(detail, "2000").getOrThrow() }
        assertTrue(streams[0].url.contains("&ratio=2000"))
    }
}
