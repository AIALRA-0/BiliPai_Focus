package com.android.purebilibili.core.store

/** 主账号仍承载互动与个人数据，其他用途只覆盖明确划定的请求。 */
enum class AccountPurpose(val storageKey: String) {
    RECOMMENDATION("recommendation_mid"),
    WATCH_HISTORY("watch_history_mid"),
    PLAYBACK("playback_mid"),
}
