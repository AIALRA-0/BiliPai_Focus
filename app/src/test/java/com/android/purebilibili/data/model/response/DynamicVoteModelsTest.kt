package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DynamicVoteModelsTest {

    @Test
    fun payloadUsesNestedVoteInfoAndMergesMyVotes() {
        val resolved = DynamicVoteInfoPayload(
            vote_info = DynamicVoteInfo(
                vote_id = 12L,
                title = "今晚吃什么",
                options = listOf(DynamicVoteOption(opt_idx = 1, opt_desc = "面"))
            ),
            my_votes = listOf(1)
        ).toResolvedVoteInfo()

        assertEquals(12L, resolved?.vote_id)
        assertEquals(listOf(1), resolved?.my_votes)
    }

    @Test
    fun payloadWithoutVoteIdIsRejected() {
        assertNull(
            DynamicVoteInfoPayload(
                vote_info = DynamicVoteInfo(title = "无效")
            ).toResolvedVoteInfo()
        )
    }
}