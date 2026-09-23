package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PugvSeasonModelsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun decodePugvSeasonResponse_andConvertToBangumiDetail() {
        val jsonStr = """
        {
          "code": 0,
          "message": "success",
          "data": {
            "season_id": 12345,
            "title": "精品课程：Kotlin Compose 架构演进",
            "subtitle": "带你深入理解现代响应式 UI",
            "cover": "https://example.com/course_cover.jpg",
            "evaluate": "系统化学习 Compose 进阶与调优",
            "ep_count": 10,
            "brief": {
              "desc": "本课程从基础原理到实战深度拆解"
            },
            "stat": {
              "views": 88888,
              "reply": 666,
              "favored_count": 9999,
              "share": 123
            },
            "up_info": {
              "mid": 1234567,
              "uname": "讲师UP主",
              "avatar": "https://example.com/avatar.jpg"
            },
            "user_status": {
              "payed": 1,
              "favored": 1,
              "progress": {
                "last_ep_id": 1002
              }
            },
            "episodes": [
              {
                "id": 1001,
                "aid": 286347735,
                "cid": 3001,
                "title": "第1讲",
                "subtitle": "Compose 渲染与重组机制",
                "cover": "https://example.com/ep1.jpg",
                "duration": 584,
                "playable": true,
                "status": 1,
                "label": "试看",
                "episode_can_view": true
              },
              {
                "id": 1002,
                "aid": 286347736,
                "cid": 3002,
                "title": "第2讲",
                "subtitle": "自定义 Layout 与 Modifier 深度解析",
                "cover": "https://example.com/ep2.jpg",
                "duration": 1200,
                "playable": false,
                "status": 1,
                "label": "",
                "episode_can_view": false
              }
            ]
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PugvSeasonResponse>(jsonStr)
        assertEquals(0, response.code)
        val data = assertNotNull(response.data)
        assertEquals(12345L, data.seasonId)
        assertEquals("精品课程：Kotlin Compose 架构演进", data.title)
        assertEquals(2, data.episodes?.size)

        // Convert to BangumiDetail
        val detail = data.toBangumiDetail()
        assertEquals(12345L, detail.seasonId)
        assertEquals("精品课程：Kotlin Compose 架构演进", detail.title)
        assertEquals(10, detail.seasonType)
        assertEquals("课堂", detail.seasonTypeName)
        assertEquals("本课程从基础原理到实战深度拆解", detail.evaluate)
        assertEquals(88888L, detail.stat?.views)
        assertEquals(9999L, detail.stat?.favorites)
        assertEquals(666L, detail.stat?.reply)
        assertEquals(1, detail.userStatus?.follow)
        assertEquals(1002L, detail.userStatus?.progress?.lastEpId)

        // Episodes
        val episodes = assertNotNull(detail.episodes)
        assertEquals(2, episodes.size)

        val ep1 = episodes[0]
        assertEquals(1001L, ep1.id)
        assertEquals(286347735L, ep1.aid)
        assertEquals("BV17f4y1R7YS", ep1.bvid)
        assertEquals(3001L, ep1.cid)
        assertEquals("第1讲", ep1.title)
        assertEquals("Compose 渲染与重组机制", ep1.longTitle)
        assertEquals(584000L, ep1.duration)
        assertEquals("试看", ep1.badge)

        val ep2 = episodes[1]
        assertEquals(1002L, ep2.id)
        assertEquals(3002L, ep2.cid)
        assertEquals("第2讲", ep2.title)
        assertEquals("自定义 Layout 与 Modifier 深度解析", ep2.longTitle)
        assertEquals(1200000L, ep2.duration)
        assertEquals("付费", ep2.badge) // Unlabeled and not playable defaults to "付费"
    }

    @Test
    fun decodeRealWorldPugvSeasonResponse_withNullFieldsAndBriefImages_andConvertToBangumiDetail() {
        val jsonStr = """
        {
          "code": 0,
          "message": "success",
          "data": {
            "season_id": 150,
            "title": "从零开始学 Python",
            "subtitle": "快速入门指南",
            "cover": "https://i0.hdslb.com/bfs/cheese/cover.jpg",
            "evaluate": null,
            "ep_count": 2,
            "brief": {
              "title": "概述",
              "content": "从语法基础到项目实战",
              "img": [
                {
                  "url": "https://i0.hdslb.com/bfs/cheese/brief1.jpg",
                  "aspect_ratio": 0.5625
                }
              ],
              "type": 1
            },
            "stat": {
              "play": 50000,
              "play_desc": "5.0万次播放",
              "views": 0,
              "reply": 100,
              "favored_count": 0,
              "share": 50
            },
            "up_info": {
              "mid": 999999,
              "uname": "Python讲师",
              "avatar": "https://i0.hdslb.com/bfs/face/teacher.jpg",
              "brief": "知名开发者",
              "follower": 120000,
              "is_follow": 1
            },
            "user_status": {
              "payed": 0,
              "favored": 1,
              "favored_count": 3500,
              "progress": null
            },
            "episodes": [
              {
                "id": 501,
                "aid": 123456,
                "cid": 789012,
                "title": "第1讲",
                "subtitle": "环境配置",
                "cover": "",
                "duration": 600,
                "from": "pugv",
                "playable": true,
                "status": 1,
                "label": null,
                "episode_can_view": true,
                "play": 20000
              },
              {
                "id": 502,
                "aid": 123457,
                "cid": 789013,
                "title": "",
                "subtitle": null,
                "cover": "",
                "duration": 900,
                "from": "pugv",
                "playable": false,
                "status": 1,
                "label": null,
                "episode_can_view": false,
                "play": 15000
              }
            ]
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PugvSeasonResponse>(jsonStr)
        assertEquals(0, response.code)
        val data = assertNotNull(response.data)
        assertEquals(150L, data.seasonId)
        assertEquals(null, data.evaluate)

        val detail = data.toBangumiDetail()
        assertEquals(150L, detail.seasonId)
        assertEquals("从零开始学 Python", detail.title)
        assertEquals(10, detail.seasonType)
        assertEquals("课堂", detail.seasonTypeName)
        assertEquals("从语法基础到项目实战", detail.evaluate)
        assertEquals(50000L, detail.stat?.views)
        assertEquals(3500L, detail.stat?.favorites)
        assertEquals(1, detail.userStatus?.follow)
        assertEquals(null, detail.userStatus?.progress?.lastEpId)
        assertNotNull(detail.upInfo)
        assertEquals(999999L, detail.upInfo?.mid)
        assertEquals("Python讲师", detail.upInfo?.uname)
        assertEquals(1, detail.briefImgs?.size)
        assertEquals(0.5625f, detail.briefImgs?.first()?.aspectRatio)

        val episodes = assertNotNull(detail.episodes)
        assertEquals(2, episodes.size)
        assertEquals("试看", episodes[0].badge)
        assertEquals("第2讲", episodes[1].title)
        assertEquals("付费", episodes[1].badge)
    }
}
