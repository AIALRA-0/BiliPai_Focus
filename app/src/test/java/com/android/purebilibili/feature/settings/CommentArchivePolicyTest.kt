package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.database.entity.CommentFraudRecord
import com.android.purebilibili.data.repository.validateCommentArchiveImport
import com.android.purebilibili.feature.settings.screen.commentArchiveSourceUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CommentArchivePolicyTest {
    private val record = CommentFraudRecord(rpid = 100L, oid = 200L, uid = 300L, message = "评论")

    @Test
    fun sourceLinksRespectSubjectIdentity() {
        assertEquals("https://www.bilibili.com/video/av200", commentArchiveSourceUrl(record))
        assertEquals("https://www.bilibili.com/read/cv200", commentArchiveSourceUrl(record.copy(type = 12)))
        assertEquals("https://t.bilibili.com/200", commentArchiveSourceUrl(record.copy(type = 17)))
        // 旧图文评论区的 OID 不是动态 ID，不生成错误的动态链接。
        assertNull(commentArchiveSourceUrl(record.copy(type = 11)))
        assertNull(commentArchiveSourceUrl(record.copy(oid = 0L)))
        assertNull(commentArchiveSourceUrl(record.copy(type = 999, origin_url = "https://example.com")))
    }

    @Test
    fun importAllowsLegacyUnknownOwnersAndRejectsInvalidIdentity() {
        validateCommentArchiveImport(listOf(record.copy(uid = 0L)))
        listOf(record.copy(rpid = 0), record.copy(oid = -1), record.copy(uid = -1),
            record.copy(type = 0), record.copy(root = -1), record.copy(parent = -1),
            record.copy(message = "x".repeat(100001))).forEach {
            assertFailsWith<IllegalArgumentException> { validateCommentArchiveImport(listOf(it)) }
        }
        assertFailsWith<IllegalArgumentException> { validateCommentArchiveImport(List(20001) { record }) }
    }
}
