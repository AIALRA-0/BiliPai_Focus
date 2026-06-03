package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser

internal sealed interface SearchSubmitAction {
    data object Ignore : SearchSubmitAction
    data class OpenSearch(val keyword: String) : SearchSubmitAction
    data class OpenNativeTarget(val target: BilibiliNavigationTarget) : SearchSubmitAction
}

internal fun resolveSearchSubmitAction(rawKeyword: String): SearchSubmitAction {
    val keyword = rawKeyword.trim()
    if (keyword.isEmpty()) return SearchSubmitAction.Ignore

    return when (val target = BilibiliNavigationTargetParser.parse(keyword)) {
        null -> SearchSubmitAction.OpenSearch(keyword)
        is BilibiliNavigationTarget.Search -> SearchSubmitAction.OpenSearch(target.keyword)
        else -> SearchSubmitAction.OpenNativeTarget(target)
    }
}
