package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.core.database.entity.BlockedUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockedUpImportPolicyTest {

    @Test
    fun `import plan skips existing mids and duplicate incoming rows`() {
        val plan = buildBlockedUpImportPlan(
            existingMids = setOf(42L),
            items = listOf(
                BlockedUpImportItem(mid = 42L, name = "已存在"),
                BlockedUpImportItem(mid = 7L, name = ""),
                BlockedUpImportItem(mid = 7L, name = "重复"),
                BlockedUpImportItem(mid = 0L, name = "无效")
            )
        )

        assertEquals(listOf(7L), plan.itemsToInsert.map { it.mid })
        assertEquals("UP主7", plan.itemsToInsert.single().name)
        assertEquals(1, plan.existingCount)
        assertEquals(2, plan.failedCount)
    }

    @Test
    fun `remote blacklist users map to import items with names and faces`() {
        val items = buildBlockedUpImportItemsFromRemoteBlacks(
            listOf(
                FollowingUser(mid = 7L, uname = "测试UP", face = "https://i0.hdslb.com/test.jpg"),
                FollowingUser(mid = 8L, uname = "   ", face = "", sign = "广告号")
            )
        )

        assertEquals(
            listOf(
                BlockedUpImportItem(mid = 7L, name = "测试UP", face = "https://i0.hdslb.com/test.jpg"),
                BlockedUpImportItem(mid = 8L, name = "UP主8", face = "", sign = "广告号")
            ),
            items
        )
    }

    @Test
    fun `remote blacklist mapping drops invalid mids before import plan`() {
        val items = buildBlockedUpImportItemsFromRemoteBlacks(
            listOf(
                FollowingUser(mid = 0L, uname = "无效"),
                FollowingUser(mid = -1L, uname = "无效"),
                FollowingUser(mid = 9L, uname = "有效")
            )
        )

        assertEquals(listOf(9L), items.map { it.mid })
    }

    @Test
    fun `blocked list write message keeps local success when remote is skipped`() {
        val message = buildBlockedUpWriteMessage(
            blocked = true,
            remoteStatus = BilibiliBlockedListRemoteStatus.SKIPPED_NOT_LOGGED_IN
        )

        assertEquals("已屏蔽该 UP 主；未登录，未同步 B站黑名单", message)
    }

    @Test
    fun `blocked list write message reports remote failure without rolling back local result`() {
        val message = buildBlockedUpWriteMessage(
            blocked = false,
            remoteStatus = BilibiliBlockedListRemoteStatus.FAILED,
            remoteMessage = "csrf 校验失败"
        )

        assertEquals("已解除屏蔽；B站黑名单同步失败：csrf 校验失败", message)
    }

    @Test
    fun `blocked list remote source maps comment area to re src 15`() {
        assertEquals(
            11,
            resolveBlockedUpRelationReSrc(BlockedUpRelationSource.PROFILE)
        )
        assertEquals(
            15,
            resolveBlockedUpRelationReSrc(BlockedUpRelationSource.COMMENT)
        )
    }

    @Test
    fun `metadata refresh message reports updated deleted and failed counts`() {
        assertEquals(
            "已刷新 2 个用户资料，1 个账号疑似已注销",
            buildBlockedUpMetadataRefreshMessage(
                updatedCount = 2,
                deletedCount = 1,
                failedCount = 0
            )
        )
        assertEquals(
            "刷新完成：2 个成功，1 个疑似已注销，3 个失败",
            buildBlockedUpMetadataRefreshMessage(
                updatedCount = 2,
                deletedCount = 1,
                failedCount = 3
            )
        )
    }

    @Test
    fun `blocked up share text includes visual metadata and profile link`() {
        val text = buildBlockedUpShareText(
            listOf(
                BlockedUp(
                    mid = 123L,
                    name = "测试UP",
                    face = "",
                    level = 5,
                    sign = "不要推荐",
                    vipLabel = "年度大会员",
                    officialTitle = "认证用户",
                    follower = 99L,
                    archiveCount = 3
                ),
                BlockedUp(
                    mid = 456L,
                    name = "",
                    face = "",
                    isDeleted = true
                )
            )
        )

        assertTrue(text.contains("BiliPai 黑名单导出（2 个用户）"))
        assertTrue(text.contains("状态: 正常 · LV5 · 年度大会员 · 认证用户"))
        assertTrue(text.contains("状态: 疑似已注销"))
        assertTrue(!text.contains("等级未知"))
        assertEquals(listOf(123L, 456L), parseBlockedUpShareText(text).map { it.mid })
    }

    @Test
    fun `blocked up share text can be parsed back for import`() {
        val text = buildBlockedUpShareText(
            listOf(
                BlockedUp(
                    mid = 123L,
                    name = "测试UP",
                    face = "https://i0.hdslb.com/test.jpg",
                    level = 5,
                    sign = "不要推荐",
                    vipLabel = "年度大会员",
                    officialTitle = "认证用户",
                    follower = 99L,
                    archiveCount = 3
                )
            )
        )

        assertEquals(
            listOf(
                BlockedUpImportItem(
                    mid = 123L,
                    name = "测试UP",
                    face = "https://i0.hdslb.com/test.jpg",
                    sign = "不要推荐",
                    level = 5,
                    vipLabel = "年度大会员",
                    officialTitle = "认证用户",
                    follower = 99L,
                    archiveCount = 3
                )
            ),
            parseBlockedUpShareText(text)
        )
    }

    @Test
    fun `blocked up share json can be parsed back for file import`() {
        val json = buildBlockedUpShareJson(
            listOf(
                BlockedUp(
                    mid = 123L,
                    name = "测试UP",
                    face = "https://i0.hdslb.com/test.jpg",
                    level = 5,
                    sign = "不要推荐",
                    vipLabel = "年度大会员",
                    officialTitle = "认证用户",
                    follower = 99L,
                    archiveCount = 3
                )
            )
        )

        assertTrue(json.trim().startsWith("{"))
        assertEquals(
            listOf(
                BlockedUpImportItem(
                    mid = 123L,
                    name = "测试UP",
                    face = "https://i0.hdslb.com/test.jpg",
                    sign = "不要推荐",
                    level = 5,
                    vipLabel = "年度大会员",
                    officialTitle = "认证用户",
                    follower = 99L,
                    archiveCount = 3
                )
            ),
            parseBlockedUpShareText(json)
        )
    }

    @Test
    fun `blocked up share parser accepts old readable text`() {
        val items = parseBlockedUpShareText(
            """
            BiliPai 黑名单导出
            UID: 123
            UID: 456
            """.trimIndent()
        )

        assertEquals(listOf(123L, 456L), items.map { it.mid })
        assertTrue(items.all { it.name.isBlank() })
    }
}
