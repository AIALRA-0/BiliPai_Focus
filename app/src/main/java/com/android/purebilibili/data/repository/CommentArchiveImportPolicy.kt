package com.android.purebilibili.data.repository

import com.android.purebilibili.core.database.entity.CommentFraudRecord

internal fun validateCommentArchiveImport(records: List<CommentFraudRecord>) {
    require(records.size <= 20000) { "单次最多导入 20000 条评论" }
    require(records.all {
        it.rpid > 0L && it.oid > 0L && it.type > 0 && it.uid >= 0L &&
            it.root >= 0L && it.parent >= 0L && it.post_time >= 0L && it.timestamp >= 0L && it.message.length <= 100000
    }) { "备份包含无效评论记录" }
}
