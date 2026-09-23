package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicCardButton
import com.android.purebilibili.data.model.response.DynamicMajor

internal data class DynamicMajorCardModel(
    val title: String,
    val subtitle: String,
    val cover: String,
    val jumpUrl: String,
    val kindLabel: String,
    val actionLabel: String = "",
    val enabled: Boolean = true,
)

internal fun resolveDynamicMajorCard(
    major: DynamicMajor?,
    darkTheme: Boolean,
): DynamicMajorCardModel? {
    if (major == null) return null

    major.common?.takeIf { it.title.isNotBlank() || it.desc.isNotBlank() }?.let { common ->
        return DynamicMajorCardModel(
            title = common.title.ifBlank { common.desc },
            subtitle = common.desc.takeUnless { it == common.title }.orEmpty(),
            cover = common.cover,
            jumpUrl = common.jump_url,
            kindLabel = common.badge?.text.orEmpty().ifBlank { common.label.ifBlank { "相关内容" } },
        )
    }
    major.music?.takeIf { it.title.isNotBlank() }?.let { music ->
        return DynamicMajorCardModel(
            title = music.title,
            subtitle = music.label,
            cover = music.cover,
            jumpUrl = music.jump_url,
            kindLabel = "音乐",
        )
    }
    major.courses?.takeIf { it.title.isNotBlank() }?.let { courses ->
        return DynamicMajorCardModel(
            title = courses.title,
            subtitle = listOf(courses.sub_title, courses.desc)
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(" · "),
            cover = courses.cover,
            jumpUrl = courses.jump_url,
            kindLabel = courses.badge?.text.orEmpty().ifBlank { "课程" },
        )
    }
    major.medialist?.takeIf { it.title.isNotBlank() }?.let { medialist ->
        return DynamicMajorCardModel(
            title = medialist.title,
            subtitle = medialist.sub_title.ifBlank { "收藏夹" },
            cover = medialist.cover,
            jumpUrl = medialist.jump_url,
            kindLabel = medialist.badge?.text.orEmpty().ifBlank { "收藏夹" },
        )
    }
    major.upower_common?.takeIf { it.title.isNotBlank() }?.let { upower ->
        val icon = if (darkTheme) upower.icon?.dark_src else upower.icon?.light_src
        return DynamicMajorCardModel(
            title = upower.title,
            subtitle = upower.title_prefix,
            cover = icon.orEmpty().ifBlank {
                if (darkTheme) upower.background?.dark_src.orEmpty() else upower.background?.light_src.orEmpty()
            },
            jumpUrl = upower.jump_url.ifBlank { upower.button?.jump_url.orEmpty() },
            kindLabel = "充电专属",
            actionLabel = resolveDynamicCardButtonLabel(upower.button),
        )
    }
    major.none?.tips?.takeIf(String::isNotBlank)?.let { tips ->
        return DynamicMajorCardModel(
            title = tips,
            subtitle = "内容暂不可用",
            cover = "",
            jumpUrl = "",
            kindLabel = "提示",
            enabled = false,
        )
    }
    return null
}

internal fun resolveDynamicCardButtonLabel(button: DynamicCardButton?): String {
    if (button == null) return ""
    return when (button.status) {
        2 -> button.check?.text
        else -> button.uncheck?.text
    }.orEmpty().ifBlank { button.jump_style?.text.orEmpty() }
}
