package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DynamicFocusFollowGroupPolicyTest {

    @Test
    fun filterDynamicItemsByFocusFollowGroups_hidesItemsFromInvisibleGroup() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = "default", name = "默认分组", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf("1002" to "quiet")
        )

        val result = filterDynamicItemsByFocusFollowGroups(
            items = listOf(
                dynamicItem(mid = 1001L),
                dynamicItem(mid = 1002L)
            ),
            config = config
        )

        assertEquals(listOf(1001L), result.map { it.modules.module_author?.mid })
    }

    @Test
    fun resolveSelectedUserIdAfterFocusFollowGroupFilter_clearsHiddenSelection() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = "default", name = "默认分组", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf("1002" to "quiet")
        )

        assertNull(resolveSelectedUserIdAfterFocusFollowGroupFilter(1002L, config))
        assertEquals(1001L, resolveSelectedUserIdAfterFocusFollowGroupFilter(1001L, config))
    }

    @Test
    fun filterPolicies_bypassWhenFilteringDisabled() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = "default", name = "默认分组", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf("1002" to "quiet")
        )

        val items = listOf(dynamicItem(mid = 1001L), dynamicItem(mid = 1002L))
        val users = listOf(
            SidebarUser(uid = 1001L, name = "UP-1001", face = ""),
            SidebarUser(uid = 1002L, name = "UP-1002", face = "")
        )

        assertEquals(2, filterDynamicItemsByFocusFollowGroups(items, config, filterEnabled = false).size)
        assertEquals(2, filterSidebarUsersByFocusFollowGroups(users, config, filterEnabled = false).size)
        assertEquals(1002L, resolveSelectedUserIdAfterFocusFollowGroupFilter(1002L, config, filterEnabled = false))
    }

    private fun dynamicItem(mid: Long): DynamicItem {
        return DynamicItem(
            id_str = mid.toString(),
            modules = DynamicModules(
                module_author = DynamicAuthorModule(
                    mid = mid,
                    name = "UP-$mid"
                )
            )
        )
    }
}
