package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RecommendationFeedbackLocalAction
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.RecommendationFeedbackType
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoActionPolicyTest {

    @Test
    fun `action sheet model matches bilibili related menu sections`() {
        val model = resolveRelatedVideoActionSheetModel(
            RelatedVideo(
                bvid = "BV1",
                title = "demo",
                owner = Owner(mid = 42, name = "达芬奇矩阵"),
                stat = Stat()
            )
        )
        assertEquals(4, model.feedbackReasons.size)
        assertTrue(model.feedbackReasons.all { it.type == RecommendationFeedbackType.FEEDBACK })
        assertEquals(
            listOf("恐怖血腥", "色情低俗", "封面恶心", "标题党/封面党"),
            model.feedbackReasons.map { it.name }
        )
        assertEquals(3, model.dislikeReasons.size)
        assertEquals("UP主：达芬奇矩阵", model.dislikeReasons[0].name)
        assertEquals(RecommendationFeedbackLocalAction.CREATOR, model.dislikeReasons[0].localAction)
        assertEquals("和当前视频无关", model.dislikeReasons[1].name)
        assertEquals("不感兴趣", model.dislikeReasons[2].name)
    }

    @Test
    fun `dislike reasons hide related card while feedback reasons keep it`() {
        assertTrue(
            shouldRemoveRelatedVideoAfterFeedback(
                RecommendationFeedbackReason(
                    name = "不感兴趣",
                    type = RecommendationFeedbackType.DISLIKE
                )
            )
        )
        assertFalse(
            shouldRemoveRelatedVideoAfterFeedback(
                RecommendationFeedbackReason(
                    name = "封面恶心",
                    type = RecommendationFeedbackType.FEEDBACK
                )
            )
        )
    }

    @Test
    fun `hidden bvids are filtered from related list`() {
        val videos = listOf(
            RelatedVideo(bvid = "BV1", title = "a", owner = Owner(), stat = Stat()),
            RelatedVideo(bvid = "BV2", title = "b", owner = Owner(), stat = Stat())
        )
        val filtered = filterRelatedVideosByHiddenBvids(videos, setOf("BV1"))
        assertEquals(listOf("BV2"), filtered.map { it.bvid })
    }
}
