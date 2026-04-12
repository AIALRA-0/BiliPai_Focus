package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicItem

internal data class DynamicCommentTarget(
    val oid: Long,
    val type: Int
)

private val DESKTOP_DYNAMIC_COMMENT_TYPES = setOf(
    "DYNAMIC_TYPE_WORD",
    "DYNAMIC_TYPE_FORWARD",
    "DYNAMIC_TYPE_LIVE_RCMD",
    "DYNAMIC_TYPE_COMMON_SQUARE",
    "DYNAMIC_TYPE_COMMON_VERTICAL"
)

private fun String.toPositiveLongOrNull(): Long? {
    return trim().toLongOrNull()?.takeIf { it > 0L }
}

private fun resolveCommentTargetFromBasic(basic: DynamicBasic?): DynamicCommentTarget? {
    if (basic == null || basic.comment_type <= 0) return null
    val oid = basic.comment_id_str.toPositiveLongOrNull()
        ?: basic.rid_str.toPositiveLongOrNull()
        ?: return null
    return DynamicCommentTarget(oid = oid, type = basic.comment_type)
}

internal fun shouldIncludeDynamicItemInVideoTab(item: DynamicItem): Boolean {
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_AV",
        "DYNAMIC_TYPE_UGC_SEASON" -> true
        else -> {
            val major = item.modules.module_dynamic?.major
            (major?.archive != null || major?.ugc_season != null) &&
                !shouldIncludeDynamicItemInPgcTab(item)
        }
    }
}

internal fun shouldIncludeDynamicItemInPgcTab(item: DynamicItem): Boolean {
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_PGC",
        "DYNAMIC_TYPE_PGC_UNION" -> true
        else -> false
    }
}

internal fun shouldIncludeDynamicItemInArticleTab(item: DynamicItem): Boolean {
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_ARTICLE",
        "DYNAMIC_TYPE_DRAW",
        "DYNAMIC_TYPE_WORD" -> true
        else -> {
            val major = item.modules.module_dynamic?.major
            major?.opus != null || major?.draw != null
        }
    }
}

internal fun shouldIncludeDynamicItemInUpTab(item: DynamicItem): Boolean {
    return !shouldIncludeDynamicItemInVideoTab(item) &&
        !shouldIncludeDynamicItemInPgcTab(item) &&
        !shouldIncludeDynamicItemInArticleTab(item)
}

internal fun resolveDynamicCommentTarget(item: DynamicItem): DynamicCommentTarget? {
    val major = item.modules.module_dynamic?.major
    when (major?.type.orEmpty()) {
        "MAJOR_TYPE_OPUS" -> {
            val oid = item.id_str.toPositiveLongOrNull() ?: return null
            return DynamicCommentTarget(oid = oid, type = 17)
        }
        "MAJOR_TYPE_ARCHIVE" -> {
            val aid = major?.archive?.aid?.toPositiveLongOrNull() ?: return null
            return DynamicCommentTarget(oid = aid, type = 1)
        }
        "MAJOR_TYPE_PGC" -> {
            val aid = major?.pgc?.aid?.toPositiveLongOrNull() ?: return null
            return DynamicCommentTarget(oid = aid, type = 1)
        }
        "MAJOR_TYPE_UGC_SEASON" -> {
            val aid = major?.ugc_season?.aid?.takeIf { it > 0L } ?: return null
            return DynamicCommentTarget(oid = aid, type = 1)
        }
        "MAJOR_TYPE_DRAW" -> {
            val drawId = major?.draw?.id?.takeIf { it > 0L } ?: return null
            return DynamicCommentTarget(oid = drawId, type = 11)
        }
    }

    val basic = item.basic
    return when (item.type.trim()) {
        "DYNAMIC_TYPE_AV",
        "DYNAMIC_TYPE_PGC",
        "DYNAMIC_TYPE_PGC_UNION",
        "DYNAMIC_TYPE_UGC_SEASON" -> {
            val oid = resolveCommentTargetFromBasic(basic)?.takeIf { it.type == 1 }?.oid
                ?: major?.archive?.aid?.toPositiveLongOrNull()
                ?: major?.pgc?.aid?.toPositiveLongOrNull()
                ?: major?.ugc_season?.aid?.takeIf { it > 0L }
                ?: return null
            DynamicCommentTarget(oid = oid, type = 1)
        }
        "DYNAMIC_TYPE_DRAW" -> {
            resolveCommentTargetFromBasic(basic)?.let { target ->
                if (target.type == 11) return target
            }
            val drawId = major?.draw?.id?.takeIf { it > 0L } ?: return item.id_str.toPositiveLongOrNull()
                ?.let { DynamicCommentTarget(oid = it, type = 17) }
            DynamicCommentTarget(oid = drawId, type = 11)
        }
        "DYNAMIC_TYPE_WORD",
        "DYNAMIC_TYPE_FORWARD",
        "DYNAMIC_TYPE_LIVE_RCMD",
        "DYNAMIC_TYPE_COMMON_SQUARE",
        "DYNAMIC_TYPE_COMMON_VERTICAL" -> {
            val oid = item.id_str.toPositiveLongOrNull()
                ?: resolveCommentTargetFromBasic(basic)?.oid
                ?: return null
            DynamicCommentTarget(oid = oid, type = 17)
        }
        "DYNAMIC_TYPE_ARTICLE" -> {
            resolveCommentTargetFromBasic(basic)?.let { target ->
                if (target.type == 12) return target
            }
            return null
        }
        "DYNAMIC_TYPE_MUSIC" -> {
            resolveCommentTargetFromBasic(basic)?.let { target ->
                if (target.type == 14) return target
            }
            return null
        }
        "DYNAMIC_TYPE_MEDIALIST" -> {
            resolveCommentTargetFromBasic(basic)?.let { target ->
                if (target.type == 19) return target
            }
            return null
        }
        else -> {
            val oid = item.id_str.toPositiveLongOrNull()
                ?: resolveCommentTargetFromBasic(basic)?.oid
                ?: return null
            DynamicCommentTarget(oid = oid, type = 17)
        }
    }
}

internal fun resolveDynamicCommentTargets(item: DynamicItem): List<DynamicCommentTarget> {
    val targets = linkedSetOf<DynamicCommentTarget>()
    val primary = resolveDynamicCommentTarget(item)
    if (primary != null) targets += primary

    val basicTarget = resolveCommentTargetFromBasic(item.basic)
    if (basicTarget != null) targets += basicTarget

    val desktopDynamicTarget = item.id_str.toPositiveLongOrNull()
        ?.takeIf {
            item.modules.module_dynamic?.major?.type == "MAJOR_TYPE_OPUS" ||
                item.type.trim() in DESKTOP_DYNAMIC_COMMENT_TYPES
        }
        ?.let { DynamicCommentTarget(oid = it, type = 17) }
    if (desktopDynamicTarget != null) targets += desktopDynamicTarget

    if (targets.isEmpty()) {
        item.orig?.let { return resolveDynamicCommentTargets(it) }
    }
    return targets.toList()
}
