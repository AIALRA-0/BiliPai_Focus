package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicMoreModule
import com.android.purebilibili.data.model.response.DynamicThreePointItem
import com.android.purebilibili.data.model.response.DynamicThreePointParams
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicTopic
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import com.android.purebilibili.data.model.response.OpusSummary
import com.android.purebilibili.data.model.response.RichTextNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicMenuActionPolicyTest {

    @Test
    fun manageActionDispatcherUsesSameBranchesForListAndDetail() {
        val visited = mutableListOf<String>()
        val dispatch: (DynamicManageAction) -> Unit = { action ->
            dispatchDynamicManageAction(
                action,
                onReport = { visited += "report" },
                onEdit = { visited += "edit" },
                onNotInterested = { visited += "not_interested" },
                onOther = { visited += "other" },
            )
        }

        dispatch(DynamicManageAction.NotInterested("1"))
        dispatch(DynamicManageAction.ToggleTop("2", false))

        assertEquals(listOf("not_interested", "other"), visited)
    }

    @Test
    fun editDraftRestoresTitleImagesTopicAndRichNodes() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(
                        text = "@测试 [doge] 正文",
                        rich_text_nodes = listOf(
                            RichTextNode(type = "RICH_TEXT_NODE_TYPE_AT", text = "@测试", rid = "22"),
                            RichTextNode(type = "RICH_TEXT_NODE_TYPE_EMOJI", text = "[doge]"),
                        ),
                    ),
                    topic = DynamicTopic(id = 7L, name = "Compose"),
                    major = DynamicMajor(
                        opus = OpusMajor(
                            title = "标题",
                            summary = OpusSummary(text = "摘要"),
                            pics = listOf(OpusPic(url = "https://i0.hdslb.com/a.jpg", width = 640, height = 480)),
                        ),
                    ),
                ),
            ),
        )

        val draft = resolveDynamicEditDraft(item)

        assertEquals("标题", draft.title)
        assertEquals(listOf("https://i0.hdslb.com/a.jpg"), draft.imageUris)
        assertEquals(22L, draft.mentions.single().uid)
        assertEquals(listOf("[doge]"), draft.emotes)
        assertEquals(7L, draft.topic?.id)
        assertEquals(640, draft.existingImages.single().img_width)
    }

    @Test
    fun notInterestedActionCarriesDynamicId() {
        val action = DynamicManageAction.NotInterested("dyn-100")
        assertEquals("dyn-100", action.dynamicId)
    }

    @Test
    fun pinnedMenuLabelFollowsCurrentTopState() {
        assertEquals("置顶", resolveDynamicPinnedMenuLabel(isCurrentlyTop = false))
        assertEquals("取消置顶", resolveDynamicPinnedMenuLabel(isCurrentlyTop = true))
    }

    @Test
    fun visibilityMenuLabelFollowsPrivateState() {
        assertEquals("设为仅自己可见", resolveDynamicVisibilityMenuLabel(isPrivate = false))
        assertEquals("设为公开", resolveDynamicVisibilityMenuLabel(isPrivate = true))
    }

    @Test
    fun visibilityActionMapsToApiValues() {
        assertEquals("private_pub", resolveDynamicVisibilityAction(isPrivate = true))
        assertEquals("public_pub", resolveDynamicVisibilityAction(isPrivate = false))
    }

    @Test
    fun replySelectionActionMapsToApiValues() {
        assertEquals(2, resolveDynamicReplySelectionAction(isCurrentlyEnabled = true))
        assertEquals(1, resolveDynamicReplySelectionAction(isCurrentlyEnabled = false))
    }

    @Test
    fun replyOpenActionMapsToApiValues() {
        assertEquals(3, resolveDynamicReplyOpenAction(isCurrentlyEnabled = true))
        assertEquals(4, resolveDynamicReplyOpenAction(isCurrentlyEnabled = false))
    }

    @Test
    fun replySubjectOidParsesFromBasicCommentId() {
        val item = DynamicItem(
            basic = DynamicBasic(comment_id_str = "112981396619958", comment_type = 17)
        )
        assertEquals(112981396619958L, resolveDynamicReplySubjectOid(item))
        assertEquals(17, resolveDynamicReplySubjectType(item))
    }

    @Test
    fun replySubjectOidRejectsBlankOrInvalidId() {
        assertNull(resolveDynamicReplySubjectOid(DynamicItem()))
        assertNull(resolveDynamicReplySubjectOid(DynamicItem(basic = DynamicBasic(comment_id_str = "abc"))))
        assertNull(resolveDynamicReplySubjectOid(DynamicItem(basic = DynamicBasic(comment_id_str = "-5"))))
    }

    @Test
    fun visibilityObjectIdBuildsJsonPayload() {
        assertEquals(
            """{"dyn_id":"123","dyn_type":1}""",
            buildDynamicVisibilityObjectId(dynamicId = "123", dynType = 1)
        )
    }

    @Test
    fun reportReasonsCoverSupportedDynamicReportTypes() {
        val reasons = resolveDynamicReportReasons()
        assertEquals(10, reasons.size)
        assertEquals("垃圾广告", reasons.first().label)
        assertEquals(0, reasons.last().type)
        assertEquals("其他", reasons.last().label)
    }

    @Test
    fun dynTypeFallsBackToThreePointParams() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_more = DynamicMoreModule(
                    three_point_items = listOf(
                        DynamicThreePointItem(
                            type = "THREE_POINT_DELETE",
                            params = DynamicThreePointParams(dyn_type = 17)
                        )
                    )
                )
            )
        )
        assertEquals(17, resolveDynamicDynType(item))
    }

    @Test
    fun dynTypeDefaultsToZeroWhenUnavailable() {
        assertEquals(0, resolveDynamicDynType(DynamicItem()))
    }

    @Test
    fun menuCapabilitiesHideAuthorActionsForOtherUsers() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 22L),
                module_more = DynamicMoreModule(
                    three_point_items = listOf(
                        DynamicThreePointItem(type = "THREE_POINT_REPORT"),
                    ),
                ),
            ),
        )

        val capabilities = resolveDynamicMenuCapabilities(item, currentUserMid = 11L)

        assertFalse(capabilities.isOwnDynamic)
        assertFalse(capabilities.canEdit)
        assertFalse(capabilities.canManageComments)
        assertTrue(capabilities.canReport)
        assertTrue(capabilities.canBlockAuthor)
    }

    @Test
    fun menuCapabilitiesReadPrivateStatusFromAuthorMenu() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 11L),
                module_more = DynamicMoreModule(
                    three_point_items = listOf(
                        DynamicThreePointItem(type = "THREE_POINT_EDIT"),
                        DynamicThreePointItem(
                            type = "THREE_POINT_PRIVATE",
                            params = DynamicThreePointParams(status = 1),
                        ),
                    ),
                ),
            ),
        )

        val capabilities = resolveDynamicMenuCapabilities(item, currentUserMid = 11L)

        assertTrue(capabilities.isOwnDynamic)
        assertTrue(capabilities.canEdit)
        assertTrue(capabilities.canSetVisibility)
        assertTrue(capabilities.isPrivate)
        assertFalse(capabilities.canReport)
    }
}
