package com.android.purebilibili.navigation3

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiliPaiNavKeyPolymorphicSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `back stack with SeasonSeriesDetail round trips`() {
        // 回归：SeasonSeriesDetail.type 与默认 class discriminator "type" 冲突，
        // onSaveInstanceState 保存导航栈时曾抛 JsonEncodingException 闪退。
        val stack: List<BiliPaiNavKey> = listOf(
            BiliPaiNavKey.MainHost,
            BiliPaiNavKey.SeasonSeriesDetail(
                type = "season",
                id = 123L,
                mid = 456L,
                title = "合集",
                ownerName = "UP主"
            ),
            BiliPaiNavKey.VideoDetail(bvid = "BV1", cid = 2L),
        )

        val encoded = json.encodeToString(ListSerializer(BiliPaiNavKey.serializer()), stack)

        // 自定义 discriminator 不与任何子类属性名冲突
        assertTrue(encoded.contains("\"nav_key_class\""))
        assertTrue(encoded.contains("\"type\":\"season\""))

        val decoded = json.decodeFromString(ListSerializer(BiliPaiNavKey.serializer()), encoded)
        assertEquals(stack, decoded)
    }
}
