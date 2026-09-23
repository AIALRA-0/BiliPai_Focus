package com.android.purebilibili.core.plugin

import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedTransformPluginPolicyTest {
    @Test
    fun transformsRunInRegistrationOrder() {
        val first = SuffixTransformPlugin("first", "-1")
        val second = SuffixTransformPlugin("second", "-2")

        val result = applyFeedTransformPlugins(
            items = listOf(VideoItem(bvid = "BV")),
            plugins = listOf(first, second),
            feedKind = FeedKind.SEARCH,
        )

        assertEquals("BV-1-2", result.single().bvid)
    }

    @Test
    fun failedTransformKeepsLastSuccessfulResult() {
        val failures = mutableListOf<String>()
        val result = applyFeedTransformPlugins(
            items = listOf(VideoItem(bvid = "BV")),
            plugins = listOf(SuffixTransformPlugin("first", "-1"), ThrowingTransformPlugin),
            feedKind = FeedKind.HOME_RECOMMEND,
            onFailure = { plugin, _ -> failures += plugin.id },
        )

        assertEquals("BV-1", result.single().bvid)
        assertEquals(listOf("throwing"), failures)
    }

    private class SuffixTransformPlugin(
        override val id: String,
        private val suffix: String,
    ) : FeedTransformPlugin {
        override val name = id
        override val description = id
        override val version = "1"

        override fun transformFeedItems(items: List<VideoItem>, feedKind: FeedKind): List<VideoItem> =
            items.map { it.copy(bvid = it.bvid + suffix) }
    }

    private object ThrowingTransformPlugin : FeedTransformPlugin {
        override val id = "throwing"
        override val name = id
        override val description = id
        override val version = "1"

        override fun transformFeedItems(items: List<VideoItem>, feedKind: FeedKind): List<VideoItem> {
            error("boom")
        }
    }
}
