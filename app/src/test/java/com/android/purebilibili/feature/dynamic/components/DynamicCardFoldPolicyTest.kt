package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicDisputeModule
import com.android.purebilibili.data.model.response.DynamicFoldModule
import com.android.purebilibili.data.model.response.DynamicFoldUser
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicTagModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicCardFoldPolicyTest {

    @Test
    fun pinnedTagShowsWhenTextPresent() {
        assertTrue(shouldShowDynamicPinnedTag(DynamicTagModule(text = "置顶")))
    }

    @Test
    fun pinnedTagHiddenWhenMissingOrBlank() {
        assertFalse(shouldShowDynamicPinnedTag(null))
        assertFalse(shouldShowDynamicPinnedTag(DynamicTagModule(text = "")))
        assertFalse(shouldShowDynamicPinnedTag(DynamicTagModule(text = "  ")))
    }

    @Test
    fun foldStatementResolvesTrimmedText() {
        assertEquals(
            "展开3条相关动态",
            resolveDynamicFoldStatement(
                DynamicFoldModule(
                    ids = listOf("1", "2", "3"),
                    statement = " 展开3条相关动态 "
                )
            )
        )
    }

    @Test
    fun foldStatementHiddenWhenMissingOrBlank() {
        assertNull(resolveDynamicFoldStatement(null))
        assertNull(resolveDynamicFoldStatement(DynamicFoldModule()))
        assertNull(resolveDynamicFoldStatement(DynamicFoldModule(statement = "   ")))
    }

    @Test
    fun unfoldingRelatedDynamicsRevealsHiddenNeighbors() {
        val items = listOf(
            DynamicItem(
                id_str = "visible-7h",
                visible = true,
                modules = DynamicModules(
                    module_fold = DynamicFoldModule(
                        ids = listOf("hidden-a", "hidden-b"),
                        statement = "展开2条相关动态",
                    )
                )
            ),
            DynamicItem(id_str = "hidden-a", visible = false),
            DynamicItem(id_str = "hidden-b", visible = false),
            DynamicItem(id_str = "visible-12h", visible = true),
        )

        val unfolded = unfoldRelatedDynamicItems(items, "visible-7h")

        assertTrue(unfolded[1].visible)
        assertTrue(unfolded[2].visible)
        assertNull(unfolded[0].modules.module_fold)
        assertTrue(unfolded[3].visible)
    }

    @Test
    fun foldStatementKeepsUsersForAvatarStack() {
        val fold = DynamicFoldModule(
            ids = listOf("1"),
            statement = "展开",
            users = listOf(
                DynamicFoldUser(mid = 1L, face = "https://a"),
                DynamicFoldUser(mid = 2L, face = "https://b")
            )
        )
        assertEquals("展开", resolveDynamicFoldStatement(fold))
        assertEquals(2, fold.users.size)
    }

    @Test
    fun disputeShowsWhenTitlePresent() {
        assertTrue(
            shouldShowDynamicDispute(
                DynamicDisputeModule(title = "视频内含有危险行为", jump_url = "//www.bilibili.com/")
            )
        )
    }

    @Test
    fun disputeShowsWhenOnlyDescPresent() {
        assertTrue(
            shouldShowDynamicDispute(
                DynamicDisputeModule(desc = "已折叠")
            )
        )
    }

    @Test
    fun disputeHiddenWhenMissingOrBlank() {
        assertFalse(shouldShowDynamicDispute(null))
        assertFalse(shouldShowDynamicDispute(DynamicDisputeModule()))
        assertFalse(shouldShowDynamicDispute(DynamicDisputeModule(title = " ", desc = " ")))
    }
}
