package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class VideoContentBadge(val text: String = "")

/** Keep endpoint metadata separate from navigation type and deduplicate server labels. */
fun resolveVideoStatusLabels(
    cooperation: Boolean = false,
    interactive: Boolean = false,
    course: Boolean = false,
    charging: Boolean = false,
    paid: Boolean = false,
    serverLabels: List<String> = emptyList(),
): List<String> = buildList {
    if (charging) add("充电专属") else if (paid) add("付费")
    if (course) add("课堂")
    if (cooperation) add("合作")
    if (interactive) add("互动")
    addAll(serverLabels.map(String::trim).filter(String::isNotBlank).map {
        when (it) {
            "课程" -> "课堂"
            "充电" -> "充电专属"
            else -> it
        }
    })
}.distinct()
