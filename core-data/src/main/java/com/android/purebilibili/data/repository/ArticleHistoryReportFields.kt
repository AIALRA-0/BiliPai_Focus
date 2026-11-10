package com.android.purebilibili.data.repository

private const val ARTICLE_HISTORY_REPORT_TYPE = 5

fun buildArticleHistoryReportFields(
    articleId: Long,
    csrf: String
): Map<String, String>? {
    if (articleId <= 0L || csrf.isBlank()) return null
    return mapOf(
        "aid" to articleId.toString(),
        "type" to ARTICLE_HISTORY_REPORT_TYPE.toString(),
        "csrf" to csrf
    )
}
