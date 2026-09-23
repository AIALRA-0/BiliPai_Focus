package com.android.purebilibili.data.model.response

import com.android.purebilibili.feature.list.resolveFavoriteCollectionRoute
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FavoriteCollectionResourceMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `favorite collection resource maps to collection display item`() {
        val response = json.decodeFromString<FavoriteResourceResponse>(
            """
            {
              "code": 0,
              "data": {
                "medias": [
                  {
                    "id": 725909,
                    "type": 21,
                    "title": "小约翰可汗高分视频",
                    "cover": "https://example.com/cover.jpg",
                    "intro": "经典视频合集",
                    "media_count": 54,
                    "upper": {
                      "mid": 96070394,
                      "name": "UP-Sings",
                      "face": "https://example.com/face.jpg"
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = requireNotNull(response.data?.medias).first().toVideoItem()

        assertTrue(item.isCollectionResource)
        assertEquals(725909L, item.collectionId)
        assertEquals(96070394L, item.collectionMid)
        assertEquals(54, item.collectionMediaCount)
    }

    @Test
    fun `resolveFavoriteCollectionRoute returns favorite season route payload for collection items`() {
        val item = VideoItem(
            title = "小约翰可汗高分视频",
            isCollectionResource = true,
            collectionId = 725909L,
            collectionMid = 96070394L
        )

        val route = resolveFavoriteCollectionRoute(item)

        assertNotNull(route)
        assertEquals("favorite_season", route.type)
        assertEquals(725909L, route.id)
        assertEquals(96070394L, route.mid)
        assertEquals("小约翰可汗高分视频", route.title)
    }
}
