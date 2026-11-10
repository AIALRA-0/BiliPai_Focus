package com.android.purebilibili.feature.live

import com.android.purebilibili.data.repository.LiveShieldInfo

/** Only the keyword/UID rules exposed by the live service are applied locally. */
internal class LiveDanmakuFilterPolicy(info: LiveShieldInfo = LiveShieldInfo()) {
    private val keywords = info.keywords.map { it.keyword }.filter { it.isNotBlank() }
    private val userIds = info.users.map { it.uid }.filter { it > 0L }.toSet()

    fun blocks(uid: Long, text: String): Boolean =
        uid in userIds || keywords.any { text.contains(it) }
}
