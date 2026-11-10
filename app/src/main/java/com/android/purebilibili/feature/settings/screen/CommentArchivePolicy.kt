package com.android.purebilibili.feature.settings.screen

import com.android.purebilibili.core.database.entity.CommentFraudRecord

/** 从已知的评论区身份生成原内容地址，不执行备份内提供的任意 URL。 */
internal fun commentArchiveSourceUrl(record: CommentFraudRecord): String? {
    if (record.oid <= 0L) return null
    return when (record.type) {
        1 -> "https://www.bilibili.com/video/av${record.oid}"
        12 -> "https://www.bilibili.com/read/cv${record.oid}"
        17 -> "https://t.bilibili.com/${record.oid}"
        else -> null
    }
}
