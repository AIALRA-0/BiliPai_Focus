package com.android.purebilibili.feature.home.subscription

import com.android.purebilibili.core.plugin.feed.isHttpFeedUrl

internal fun buildSubscriptionArticleShareText(title: String, sourceTitle: String, link: String): String =
    listOf(title, sourceTitle, link.takeIf(::isHttpFeedUrl).orEmpty())
        .map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n")
