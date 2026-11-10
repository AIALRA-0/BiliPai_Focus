package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicAuthorSearchPolicyTest {
    @Test
    fun `partial names match and exact names rank first`() {
        val users = listOf(user(1, "爱看小明"), user(2, "小明日记"), user(3, "小明"), user(4, "小红"))
        assertEquals(listOf(3L, 2L, 1L), searchDynamicAuthors(users, " 小明 ").map { it.uid })
    }

    @Test
    fun `Latin names ignore case and duplicate identities appear once`() {
        val users = listOf(user(1, "HelloUP"), user(1, "HelloUP"), user(2, "另一个 UP"))
        assertEquals(listOf(1L), searchDynamicAuthors(users, "helloup").map { it.uid })
        assertTrue(searchDynamicAuthors(users, "不存在").isEmpty())
    }

    @Test
    fun `empty search retains order and skips shortcuts and empty names`() {
        val users = listOf(user(-1, "全部"), user(2, "乙"), user(1, "甲"), user(3, " "))
        assertEquals(listOf(2L, 1L), searchDynamicAuthors(users, " ").map { it.uid })
    }

    private fun user(uid: Long, name: String) = SidebarUser(uid = uid, name = name, face = "")
}
