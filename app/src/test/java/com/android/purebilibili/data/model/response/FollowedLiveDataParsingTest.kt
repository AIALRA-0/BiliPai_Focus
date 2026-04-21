package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowedLiveDataParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `followed live data accepts live_count alias`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [],
                "live_count": 7,
                "count": 12
              }
            }
            """.trimIndent()
        )

        assertEquals(7, response.data?.livingNum)
        assertEquals(12, response.data?.notLivingNum)
    }
}
