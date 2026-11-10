package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleCommentNavigationTest {
    @Test
    fun nativeAndWebLinksPreserveCommentLocation() {
        listOf(
            "https://www.bilibili.com/read/cv123?comment_root_id=456#reply789",
            "bilibili://article/123?root=456&comment_id=789",
            "bilibili://browser/?url=https%3A%2F%2Fwww.bilibili.com%2Fread%2Fcv123%3Froot_id%3D456%26comment_id%3D789",
        ).forEach { link ->
            assertEquals(BilibiliNavigationTarget.Article(123L, 456L, 789L), BilibiliNavigationTargetParser.parse(link), link)
        }
    }

    @Test
    fun invalidCommentIdsDoNotTurnReadingIntoCommentEntry() {
        assertEquals(
            BilibiliNavigationTarget.Article(123L),
            BilibiliNavigationTargetParser.parse("https://www.bilibili.com/read/cv123?root_id=-1&comment_id=invalid"),
        )
    }

    @Test
    fun notificationCommentLinksKeepArticleIdentityEvenWhenEntryUriMigratesToOpus() {
        listOf(
            "bilibili://comment/msg_fold/12/123/456/789?enterUri=https%3A%2F%2Fwww.bilibili.com%2Fopus%2F999",
            "https://www.bilibili.com/h5/comment/sub?oid=123&pageType=12&root_reply_id=456&comment_id=789",
        ).forEach { link ->
            assertEquals(BilibiliNavigationTarget.Article(123L, 456L, 789L), BilibiliNavigationTargetParser.parse(link), link)
        }
    }

    @Test
    fun targetOnlyAndRootOnlyLocationsRemainDistinct() {
        listOf(
            "https://www.bilibili.com/read/cv123?comment_id=789",
            "https://www.bilibili.com/read/cv123#reply%37%38%39",
            "bilibili://comment/detail/12/123/0/789",
        ).forEach { link ->
            assertEquals(BilibiliNavigationTarget.Article(123L, 0L, 789L), BilibiliNavigationTargetParser.parse(link), link)
        }
        assertEquals(
            BilibiliNavigationTarget.Article(123L, 456L, 0L),
            BilibiliNavigationTargetParser.parse("https://www.bilibili.com/read/cv123?root=456"),
        )
    }

    @Test
    fun encodedQueryTargetHasPriorityOverFragmentAndEqualRootIsNormalized() {
        assertEquals(
            BilibiliNavigationTarget.Article(123L, 456L, 789L),
            BilibiliNavigationTargetParser.parse("https://www.bilibili.com/read/cv123?root=%34%35%36&comment_id=%37%38%39#reply999"),
        )
        assertEquals(
            BilibiliNavigationTarget.Article(123L, 456L, 0L),
            BilibiliNavigationTargetParser.parse("bilibili://comment/detail/12/123/456?comment_id=456"),
        )
    }

    @Test
    fun unrelatedOrOverflowingFragmentsDoNotCreateCommentLocation() {
        listOf("notreply789", "reply789garbage", "reply0", "reply9223372036854775808").forEach { fragment ->
            assertEquals(
                BilibiliNavigationTarget.Article(123L),
                BilibiliNavigationTargetParser.parse("https://www.bilibili.com/read/cv123#$fragment"),
                fragment,
            )
        }
    }
}
