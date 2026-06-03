package com.android.purebilibili.core.plugin.json

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse

class RuleEngineTest {

    @Test
    fun shouldShowVideo_exposesAdditionalFeedFieldsForFiltering() {
        val video = VideoItem(
            bvid = "BV1",
            title = "测试视频",
            owner = Owner(mid = 42L, name = "UP-A"),
            stat = Stat(view = 1_000, reply = 8),
            tid = 36,
            tname = "知识"
        )
        val rules = listOf(
            Rule(field = "stat.reply", op = "ge", value = JsonPrimitive(5), action = "hide"),
            Rule(field = "tid", op = "eq", value = JsonPrimitive(36), action = "hide"),
            Rule(field = "tname", op = "eq", value = JsonPrimitive("知识"), action = "hide")
        )

        assertFalse(RuleEngine.shouldShowVideo(video, rules))
    }
}
