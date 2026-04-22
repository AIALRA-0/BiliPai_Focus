package com.android.purebilibili.feature.dynamic

internal fun shouldApplyTimelineFeedResult(
    currentRequestType: String,
    requestType: String,
    activeRequestToken: Long,
    requestToken: Long
): Boolean {
    return currentRequestType == requestType && activeRequestToken == requestToken
}
