package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCreateModelsTest {

    @Test
    fun imageDraftUsesDrawScene() {
        assertEquals(2, resolveDynamicCreateScene(hasImages = true))
        assertEquals(1, resolveDynamicCreateScene(hasImages = false))
    }

    @Test
    fun voteContentUsesBizTypeFour() {
        val contents = buildDynamicCreateContents(
            text = "来投票",
            voteId = 99L,
            voteTitle = "晚饭"
        )

        assertEquals(1, contents.first().type)
        assertEquals("来投票", contents.first().raw_text)
        assertEquals(4, contents[1].type)
        assertEquals("99", contents[1].biz_id)
        assertEquals("晚饭", contents[1].raw_text)
    }

    @Test
    fun emptyTextAndVoteStillCreatesVoteNode() {
        val contents = buildDynamicCreateContents(
            text = "  ",
            voteId = 7L,
            voteTitle = ""
        )

        assertEquals(4, contents.first().type)
        assertEquals("投票", contents.first().raw_text)
        assertTrue(contents.last().raw_text.isNotEmpty())
    }

    @Test
    fun createdDynamicIdPrefersStringField() {
        assertEquals(
            "abc",
            resolveCreatedDynamicId(DynamicCreateFeedData(dyn_id = 1L, dyn_id_str = "abc"))
        )
        assertEquals(
            "1",
            resolveCreatedDynamicId(DynamicCreateFeedData(dyn_id = 1L))
        )
    }

    @Test
    fun mentionAndEmoteBecomeStructuredContentNodes() {
        val contents = buildDynamicCreateContents(
            text = "你好 @测试用户 [doge]",
            voteId = 0L,
            voteTitle = "",
            mentions = listOf(DynamicPublishMention(uid = 42L, name = "测试用户")),
            emotes = listOf("[doge]"),
        )

        assertEquals(listOf(1, 2, 9), contents.map { it.type })
        assertEquals("42", contents[1].biz_id)
        assertEquals("@测试用户 ", contents[1].raw_text)
    }

    @Test
    fun mentionAtEndStillBecomesStructuredNode() {
        val contents = buildDynamicCreateContents(
            text = "你好 @测试用户",
            voteId = 0L,
            voteTitle = "",
            mentions = listOf(DynamicPublishMention(uid = 42L, name = "测试用户")),
        )

        assertEquals(2, contents.last().type)
        assertEquals("42", contents.last().biz_id)
    }

    @Test
    fun longerMentionWinsWhenNamesShareAPrefix() {
        val contents = buildDynamicCreateContents(
            text = "@小明同学 你好",
            voteId = 0L,
            voteTitle = "",
            mentions = listOf(
                DynamicPublishMention(uid = 1L, name = "小明"),
                DynamicPublishMention(uid = 2L, name = "小明同学"),
            ),
        )

        assertEquals(2, contents.first().type)
        assertEquals("2", contents.first().biz_id)
        assertEquals("@小明同学 ", contents.first().raw_text)
    }
}
