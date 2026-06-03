package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteModelsMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `toVideoItem maps cid from ugc first_cid`() {
        val response = json.decodeFromString<FavoriteResourceResponse>(
            """
            {
              "code": 0,
              "data": {
                "medias": [
                  {
                    "id": 371494037,
                    "bvid": "BV1CZ4y1T7gC",
                    "title": "test",
                    "cover": "https://example.com/cover.jpg",
                    "duration": 546,
                    "upper": {
                      "mid": 686127,
                      "name": "籽岷",
                      "face": "https://example.com/face.jpg"
                    },
                    "cnt_info": {
                      "play": 1638040,
                      "danmaku": 7697,
                      "collect": 11256
                    },
                    "ugc": {
                      "first_cid": 216576581
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = requireNotNull(response.data?.medias).first().toVideoItem()
        assertEquals(216576581L, item.cid)
    }

    @Test
    fun `toVideoItem falls back to bv_id when bvid is blank`() {
        val response = json.decodeFromString<FavoriteResourceResponse>(
            """
            {
              "code": 0,
              "data": {
                "medias": [
                  {
                    "id": 371494037,
                    "bvid": "",
                    "bv_id": "BV1CZ4y1T7gC",
                    "title": "test",
                    "cover": "https://example.com/cover.jpg",
                    "duration": 546
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = requireNotNull(response.data?.medias).first().toVideoItem()
        assertEquals("BV1CZ4y1T7gC", item.bvid)
    }

    @Test
    fun `toVideoItem keeps favorite playback progress metadata`() {
        val response = json.decodeFromString<FavoriteResourceResponse>(
            """
            {
              "code": 0,
              "data": {
                "medias": [
                  {
                    "id": 371494037,
                    "bvid": "BV1CZ4y1T7gC",
                    "title": "test",
                    "cover": "https://example.com/cover.jpg",
                    "duration": 546,
                    "progress": 123,
                    "view_at": 1712345678,
                    "ugc": {
                      "first_cid": 216576581
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = requireNotNull(response.data?.medias).first().toVideoItem()

        assertEquals(123, item.progress)
        assertEquals(1712345678L, item.view_at)
    }

    @Test
    fun `toVideoItem uses owner fallback when favorite resource upper is missing`() {
        val item = FavoriteData(
            id = 371494037,
            bvid = "BV1CZ4y1T7gC",
            title = "test"
        ).toVideoItem(
            ownerFallbackMid = 39366561L,
            ownerFallbackName = "阿虎医考"
        )

        assertEquals(39366561L, item.owner.mid)
        assertEquals("阿虎医考", item.owner.name)
    }

    @Test
    fun `favorite folder response keeps subscribed folder upper owner`() {
        val response = json.decodeFromString<FavFolderResponse>(
            """
            {
              "code": 0,
              "data": {
                "count": 1,
                "list": [
                  {
                    "id": 496307088,
                    "fid": 4963070,
                    "mid": 412466388,
                    "title": "入站必刷",
                    "upper": {
                      "mid": 412466388,
                      "name": "热门菌",
                      "face": ""
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val folder = requireNotNull(response.data?.list).single()
        assertEquals(412466388L, folder.upper?.mid)
        assertEquals("热门菌", folder.upper?.name)
    }
}
