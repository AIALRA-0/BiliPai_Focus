package com.android.purebilibili.feature.plugin

import com.android.purebilibili.data.model.response.SponsorCategory
import kotlinx.serialization.Serializable
import java.net.URI
import java.util.UUID

/** Per-category playback behavior, modeled after SponsorBlock-compatible clients. */
@Serializable
enum class SponsorBlockSegmentBehavior(val label: String) {
    AUTOMATIC("总是跳过"),
    SKIP_ONCE("本次跳过"),
    MANUAL("手动跳过"),
    MARKER_ONLY("仅显示"),
    DISABLED("忽略")
}

internal data class SponsorBlockCategorySetting(
    val category: String,
    val title: String,
    val description: String,
    val behavior: SponsorBlockSegmentBehavior,
    val defaultColorHex: String,
    val allowedActionTypes: List<String>,
)

internal fun defaultSponsorBlockCategoryBehaviors(
    autoSkip: Boolean = true,
    skipSponsor: Boolean = true,
    skipIntro: Boolean = true,
    skipOutro: Boolean = true,
    skipInteraction: Boolean = true
): Map<String, SponsorBlockSegmentBehavior> {
    val fallback = if (autoSkip) SponsorBlockSegmentBehavior.AUTOMATIC else SponsorBlockSegmentBehavior.MANUAL
    return SponsorCategory.ALL_SKIP_CATEGORIES.associateWith { category ->
        when (category) {
            SponsorCategory.SPONSOR -> if (skipSponsor) fallback else SponsorBlockSegmentBehavior.DISABLED
            SponsorCategory.INTRO -> if (skipIntro) fallback else SponsorBlockSegmentBehavior.DISABLED
            SponsorCategory.OUTRO -> if (skipOutro) fallback else SponsorBlockSegmentBehavior.DISABLED
            SponsorCategory.INTERACTION -> if (skipInteraction) fallback else SponsorBlockSegmentBehavior.DISABLED
            else -> fallback
        }
    }
}

internal fun resolveSponsorBlockSegmentBehavior(
    category: String,
    rawBehaviors: Map<String, String>,
    fallback: SponsorBlockSegmentBehavior
): SponsorBlockSegmentBehavior {
    return rawBehaviors[category]
        ?.let { raw -> SponsorBlockSegmentBehavior.entries.firstOrNull { it.name == raw } }
        ?: fallback
}

internal fun resolveSponsorBlockCategorySettings(config: SponsorBlockConfig): List<SponsorBlockCategorySetting> {
    return SponsorCategory.ALL_CATEGORIES.map { category ->
        SponsorBlockCategorySetting(
            category = category,
            title = SponsorCategory.getCategoryName(category),
            description = resolveSponsorBlockCategoryDescription(category),
            behavior = config.behaviorFor(category),
            defaultColorHex = defaultSponsorBlockCategoryColor(category),
            allowedActionTypes = sponsorBlockAllowedActionTypes(category),
        )
    }
}

internal fun resolveSponsorBlockCategoryDescription(category: String): String = when (category) {
    SponsorCategory.SPONSOR -> "付费推广与口播恰饭"
    SponsorCategory.SELFPROMO -> "关注、点赞、商品或创作者推广"
    SponsorCategory.INTRO -> "无实际内容的开场或过场动画"
    SponsorCategory.OUTRO -> "鸣谢、结束画面与片尾"
    SponsorCategory.INTERACTION -> "一键三连、关注等互动提醒"
    SponsorCategory.PREVIEW -> "内容预告、回顾与重复片段"
    SponsorCategory.FILLER -> "与正片无关的填充或跑题内容"
    SponsorCategory.EXCLUSIVE_ACCESS -> "整段标记为独家访问、品牌合作或抢先体验"
    SponsorCategory.PADDING -> "前黑、后黑等无内容填充"
    SponsorCategory.MUSIC_OFFTOPIC -> "音乐视频中的非音乐内容"
    SponsorCategory.POI_HIGHLIGHT -> "精彩时刻或重点位置，仅作进度提示"
    else -> "社区标注的可选片段"
}

internal fun normalizeSponsorBlockServerUrl(raw: String): String? {
    val value = raw.trim().trimEnd('/')
    if (value.isBlank()) return null
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) return null
    return value
}

internal fun generateSponsorBlockUserId(): String = UUID.randomUUID().toString().replace("-", "")

/** Same minimum format expected by BiliPai and SponsorBlock-compatible servers. */
internal fun validateSponsorBlockUserId(value: String): String? {
    val normalized = value.trim()
    return when {
        normalized.length < 30 -> "用户 ID 至少需要 30 个字符"
        !normalized.all(Char::isLetterOrDigit) -> "用户 ID 只能包含字母和数字"
        else -> null
    }
}

internal fun defaultSponsorBlockCategoryColor(category: String): String = when (category) {
    SponsorCategory.SPONSOR -> "#00D400"
    SponsorCategory.SELFPROMO -> "#FFFF00"
    SponsorCategory.EXCLUSIVE_ACCESS -> "#008A5C"
    SponsorCategory.INTERACTION -> "#CC00FF"
    SponsorCategory.POI_HIGHLIGHT -> "#FF1684"
    SponsorCategory.INTRO -> "#00FFFF"
    SponsorCategory.OUTRO -> "#0202ED"
    SponsorCategory.PREVIEW -> "#008FD6"
    SponsorCategory.PADDING -> "#222222"
    SponsorCategory.FILLER -> "#7300FF"
    SponsorCategory.MUSIC_OFFTOPIC -> "#FF9900"
    else -> "#FDE68A"
}

internal fun sponsorBlockAllowedActionTypes(category: String): List<String> = when (category) {
    SponsorCategory.SPONSOR, SponsorCategory.SELFPROMO -> listOf("skip", "mute", "full")
    SponsorCategory.EXCLUSIVE_ACCESS -> listOf("full")
    SponsorCategory.POI_HIGHLIGHT -> listOf("poi")
    SponsorCategory.INTERACTION, SponsorCategory.INTRO, SponsorCategory.OUTRO,
    SponsorCategory.PREVIEW, SponsorCategory.FILLER -> listOf("skip", "mute")
    SponsorCategory.PADDING, SponsorCategory.MUSIC_OFFTOPIC -> listOf("skip")
    else -> listOf("skip")
}

internal fun shouldUploadSponsorBlockView(config: SponsorBlockConfig): Boolean {
    return config.communityTrackingEnabled && config.userId.isNotBlank()
}
