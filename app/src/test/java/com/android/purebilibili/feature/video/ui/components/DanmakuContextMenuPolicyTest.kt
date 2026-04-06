package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuContextMenuPolicyTest {

    @Test
    fun recallConfirmationPreview_trimsAndEllipsizesLongText() {
        val preview = resolveDanmakuRecallConfirmationPreview(
            "  这是一条很长很长很长很长的弹幕内容  "
        )

        assertEquals("这是一条很长很长很长很长的弹幕...", preview)
    }

    @Test
    fun blockActionFeedbackMessage_distinguishesAddedAndExistingKeywordRules() {
        assertEquals(
            "已加入屏蔽词，可在屏蔽管理里编辑",
            resolveDanmakuBlockActionFeedbackMessage(
                target = DanmakuBlockActionTarget.KEYWORD,
                changed = true
            )
        )
        assertEquals(
            "该屏蔽词已存在",
            resolveDanmakuBlockActionFeedbackMessage(
                target = DanmakuBlockActionTarget.KEYWORD,
                changed = false
            )
        )
    }

    @Test
    fun blockActionFeedbackMessage_distinguishesAddedAndExistingUserRules() {
        assertEquals(
            "已屏蔽该发送者，可在屏蔽管理里编辑",
            resolveDanmakuBlockActionFeedbackMessage(
                target = DanmakuBlockActionTarget.USER,
                changed = true
            )
        )
        assertEquals(
            "该发送者已在屏蔽列表中",
            resolveDanmakuBlockActionFeedbackMessage(
                target = DanmakuBlockActionTarget.USER,
                changed = false
            )
        )
    }
}
