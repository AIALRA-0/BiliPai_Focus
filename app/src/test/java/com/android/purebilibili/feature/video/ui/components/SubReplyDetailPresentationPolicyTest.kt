package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.data.model.response.ReplyContent
import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyMember
import com.android.purebilibili.data.model.response.ReplyPage
import com.android.purebilibili.data.model.response.ReplySailingCardBg
import com.android.purebilibili.data.model.response.ReplySailingFan
import com.android.purebilibili.data.model.response.ReplyUserSailing
import com.android.purebilibili.feature.video.viewmodel.resolveSubReplyLoadedTotalCount
import com.android.purebilibili.feature.video.viewmodel.resolveSubReplyRemoteTotalCount
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubReplyDetailPresentationPolicyTest {

    @Test
    fun `section title should include current reply count`() {
        assertEquals("相关回复共14条", resolveSubReplyDetailSectionTitle(replyCount = 14))
    }

    @Test
    fun `sub reply detail reveal motion staggers by hierarchy`() {
        assertEquals(40, resolveSubReplyDetailRevealDelayMillis(levelIndex = 0))
        assertEquals(95, resolveSubReplyDetailRevealDelayMillis(levelIndex = 1))
        assertEquals(150, resolveSubReplyDetailRevealDelayMillis(levelIndex = 2))
        assertEquals(360, resolveSubReplyDetailRevealDelayMillis(levelIndex = 20))
    }

    @Test
    fun `sub reply target list index resolves root and child positions`() {
        val replies = listOf(
            ReplyItem(rpid = 20L),
            ReplyItem(rpid = 30L)
        )

        assertEquals(
            0,
            resolveSubReplyTargetListIndex(
                rootReplyId = 10L,
                visibleReplies = replies,
                targetReplyId = 10L
            )
        )
        assertEquals(
            2,
            resolveSubReplyTargetListIndex(
                rootReplyId = 10L,
                visibleReplies = replies,
                targetReplyId = 30L
            )
        )
        assertEquals(
            null,
            resolveSubReplyTargetListIndex(
                rootReplyId = 10L,
                visibleReplies = replies,
                targetReplyId = 99L
            )
        )
    }

    @Test
    fun `sub reply detail reveal motion stays blur free`() {
        val spec = resolveSubReplyDetailRevealSpec(levelIndex = 2)

        assertEquals(150, spec.delayMillis)
        assertEquals(300, spec.durationMillis)
        assertEquals(0f, spec.initialBlurRadiusDp)
        assertEquals(14, spec.initialOffsetDp)
    }

    @Test
    fun `sub reply detail component does not apply compose blur`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplyDetailComponents.kt").readText()

        assertFalse(source.contains(".blur("))
        assertFalse(source.contains("getCommentSubReplyRevealBlurEnabled"))
        assertFalse(source.contains("animateBounds"))
    }

    @Test
    fun `sub reply detail list avoids per reply reveal wrappers and eager footer loading`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplyDetailComponents.kt").readText()

        assertFalse(source.contains("revealKey = \"reply_"))
        assertFalse(source.contains("LaunchedEffect(isLoading, isEnd)"))
    }

    @Test
    fun `sub reply detail removes centered down arrow drag handle`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplyDetailComponents.kt").readText()

        assertFalse(source.contains("SubReplyDismissDragHandle("))
        assertFalse(source.contains("rememberAppChevronDownIcon"))
        assertFalse(source.contains("contentDescription = \"下拉关闭楼中楼\""))
    }

    @Test
    fun `sub reply detail display count keeps root declared total when page only loaded partially`() {
        assertEquals(
            8,
            resolveSubReplyDetailDisplayCount(
                rootReply = ReplyItem(rcount = 8, replies = listOf(ReplyItem(rpid = 1))),
                loadedReplyCount = 3
            )
        )
        assertEquals(
            5,
            resolveSubReplyDetailDisplayCount(
                rootReply = ReplyItem(rcount = 2),
                loadedReplyCount = 5
            )
        )
    }

    @Test
    fun `sub reply detail display count uses remote total from loaded detail response`() {
        assertEquals(
            12,
            resolveSubReplyDetailDisplayCount(
                rootReply = ReplyItem(rcount = 2),
                loadedReplyCount = 4,
                remoteReplyCount = 12
            )
        )
    }

    @Test
    fun `sub reply detail display count lets detail response correct stale root preview count`() {
        assertEquals(
            1,
            resolveSubReplyDetailDisplayCount(
                rootReply = ReplyItem(count = 2, rcount = 2),
                loadedReplyCount = 1,
                remoteReplyCount = 1
            )
        )
    }

    @Test
    fun `sub reply loaded total count prefers remote detail count over stale root preview count`() {
        assertEquals(
            12,
            resolveSubReplyLoadedTotalCount(
                rootReply = ReplyItem(count = 2, rcount = 2),
                loadedReplyCount = 4,
                remoteReplyCount = 12
            )
        )
    }

    @Test
    fun `sub reply loaded total count lets detail response correct stale root preview count`() {
        assertEquals(
            1,
            resolveSubReplyLoadedTotalCount(
                rootReply = ReplyItem(count = 2, rcount = 2),
                loadedReplyCount = 1,
                remoteReplyCount = 1
            )
        )
    }

    @Test
    fun `sub reply remote total count follows reply detail page count before root count`() {
        val data = ReplyData(
            cursor = ReplyCursor(allCount = 2),
            page = ReplyPage(count = 1),
            root = ReplyItem(count = 2, rcount = 1)
        )

        assertEquals(1, resolveSubReplyRemoteTotalCount(data))
    }

    @Test
    fun `conversation section title should include filtered reply count`() {
        assertEquals("对话共2条", resolveSubReplyConversationSectionTitle(replyCount = 2))
    }

    @Test
    fun `conversation action should only show for directed reply text`() {
        assertTrue(
            shouldShowSubReplyConversationAction(
                buildReply(message = "回复 @前进四放映室：没错")
            )
        )
        assertFalse(
            shouldShowSubReplyConversationAction(
                buildReply(message = "又又又又更新？？？？")
            )
        )
    }

    @Test
    fun `conversation action should not reuse reply composer when handler is missing`() {
        val directedReply = buildReply(message = "回复 @前进四放映室：没错")

        assertFalse(
            shouldRenderSubReplyConversationAction(
                item = directedReply,
                hasConversationHandler = false
            )
        )
        assertTrue(
            shouldRenderSubReplyConversationAction(
                item = directedReply,
                hasConversationHandler = true
            )
        )
    }

    @Test
    fun `conversation items should filter by dialog id`() {
        val first = buildReply(
            rpid = 10,
            message = "回复 @甲：第一条",
            dialog = 100
        )
        val second = buildReply(
            rpid = 11,
            message = "回复 @乙：第二条",
            dialog = 100
        )
        val other = buildReply(
            rpid = 12,
            message = "回复 @丙：无关",
            dialog = 200
        )

        assertEquals(
            listOf(10L, 11L),
            resolveSubReplyConversationItems(
                anchorReply = first,
                subReplies = listOf(first, second, other)
            ).map { it.rpid }
        )
    }

    @Test
    fun `conversation items should fallback to clicked reply when dialog is unavailable`() {
        val clicked = buildReply(
            rpid = 10,
            message = "回复 @甲：第一条"
        )

        assertEquals(
            listOf(10L),
            resolveSubReplyConversationItems(
                anchorReply = clicked,
                subReplies = emptyList()
            ).map { it.rpid }
        )
    }

    @Test
    fun `list scroll reset key changes when entering conversation mode`() {
        assertEquals(
            SubReplyDetailListScrollResetKey(
                rootReplyId = 1L,
                conversationMode = false,
                firstConversationReplyId = null
            ),
            resolveSubReplyDetailListScrollResetKey(
                rootReplyId = 1L,
                effectiveConversationMode = false,
                visibleReplies = listOf(buildReply(rpid = 10, message = "回复 @甲：第一条"))
            )
        )
        assertEquals(
            SubReplyDetailListScrollResetKey(
                rootReplyId = 1L,
                conversationMode = true,
                firstConversationReplyId = 10L
            ),
            resolveSubReplyDetailListScrollResetKey(
                rootReplyId = 1L,
                effectiveConversationMode = true,
                visibleReplies = listOf(buildReply(rpid = 10, message = "回复 @甲：第一条"))
            )
        )
    }

    @Test
    fun `auxiliary label should prefer garb card number when available`() {
        assertEquals(
            "CO.013992",
            resolveSubReplyAuxiliaryLabel(
                item = buildReply(
                    message = "test",
                    garbCardNumber = "13992"
                )
            )
        )
    }

    @Test
    fun `auxiliary label should stay hidden when no garb card number exists`() {
        assertEquals(
            null,
            resolveSubReplyAuxiliaryLabel(
                item = buildReply(message = "test")
            )
        )
    }

    @Test
    fun `auxiliary label should read user sailing fan number when legacy garb field is missing`() {
        assertEquals(
            "CO.008502",
            resolveSubReplyAuxiliaryLabel(
                item = buildReply(
                    message = "test",
                    userSailingV2 = ReplyUserSailing(
                        cardBg = ReplySailingCardBg(
                            image = "https://example.com/fan.png",
                            fan = ReplySailingFan(
                                isFan = 1,
                                number = 8502,
                                color = "#7B7F88",
                                name = "测试装扮",
                                numDesc = "008502"
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `auxiliary badge visual spec keeps decoration legible`() {
        val spec = resolveSubReplyAuxiliaryBadgeVisualSpec()

        assertEquals(46, spec.imageSizeDp)
        assertEquals(12, spec.imageCornerRadiusDp)
        assertEquals(8, spec.imageLabelSpacingDp)
        assertEquals(12, spec.labelFontSizeSp)
        assertEquals(12, spec.labelLineHeightSp)
    }

    @Test
    fun `light theme detail appearance should follow theme surface instead of dark palette`() {
        val appearance = resolveSubReplyDetailAppearance(
            surfaceColor = Color(0xFFFFFFFF),
            surfaceVariantColor = Color(0xFFF1F2F4),
            surfaceContainerHighColor = Color(0xFFE8EAF0),
            outlineVariantColor = Color(0xFFD9DCE3),
            onSurfaceColor = Color(0xFF1B1C1F),
            onSurfaceVariantColor = Color(0xFF6A6F76),
            primaryColor = Color(0xFFFB7299),
            onPrimaryColor = Color(0xFFFFFFFF)
        )

        assertEquals(Color(0xFFFFFFFF), appearance.panelColor)
        assertEquals(Color(0xFF1B1C1F), appearance.primaryTextColor)
        assertEquals(Color(0xFF6A6F76), appearance.secondaryTextColor)
        assertEquals(Color(0xFFD9DCE3), appearance.dividerColor)
        assertEquals(Color(0xFFE8EAF0), appearance.sectionDividerColor)
        assertEquals(Color(0xFFFB7299), appearance.accentColor)
    }

    @Test
    fun `dark theme detail appearance should follow active theme colors`() {
        val appearance = resolveSubReplyDetailAppearance(
            surfaceColor = Color(0xFF141414),
            surfaceVariantColor = Color(0xFF242424),
            surfaceContainerHighColor = Color(0xFF1E1E1E),
            outlineVariantColor = Color(0xFF333333),
            onSurfaceColor = Color(0xFFF5F5F5),
            onSurfaceVariantColor = Color(0xFFD0D0D0),
            primaryColor = Color(0xFFFB7299),
            onPrimaryColor = Color(0xFF101010)
        )

        assertEquals(Color(0xFF141414), appearance.panelColor)
        assertEquals(Color(0xFFF5F5F5), appearance.primaryTextColor)
        assertEquals(Color(0xFFD0D0D0), appearance.secondaryTextColor)
        assertEquals(Color(0xFF333333), appearance.dividerColor)
        assertEquals(Color(0xFF242424), appearance.placeholderColor)
    }

    private fun buildReply(
        rpid: Long = 200L,
        message: String,
        garbCardNumber: String = "",
        userSailingV2: ReplyUserSailing? = null,
        parent: Long = 0L,
        dialog: Long = 0L
    ): ReplyItem {
        return ReplyItem(
            rpid = rpid,
            parent = parent,
            dialog = dialog,
            member = ReplyMember(
                mid = "12",
                uname = "ReplyUser",
                garbCardNumber = garbCardNumber,
                userSailingV2 = userSailingV2
            ),
            content = ReplyContent(message = message)
        )
    }
}
