package com.android.purebilibili.feature.dynamic

internal fun shouldApplyTimelineFeedResult(
    currentRequestType: String,
    requestType: String,
    activeRequestToken: Long,
    requestToken: Long
): Boolean {
    return currentRequestType == requestType && activeRequestToken == requestToken
}

internal fun shouldApplyTimelineFeedResult(
    activeRequestTokens: Map<String, Long>,
    requestType: String,
    requestToken: Long
): Boolean {
    return activeRequestTokens[requestType] == requestToken
}
