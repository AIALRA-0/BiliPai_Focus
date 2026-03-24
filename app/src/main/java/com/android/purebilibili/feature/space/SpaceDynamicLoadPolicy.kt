package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.ArchiveStat
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.DrawMajor
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.EmojiInfo
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import com.android.purebilibili.data.model.response.OpusSummary
import com.android.purebilibili.data.model.response.RichTextNode
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.StatItem

enum class SpaceDynamicPresentationState {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
}

internal fun shouldRequestInitialSpaceDynamicLoad(
    hasLoadedOnce: Boolean,
    isLoading: Boolean
): Boolean {
    return !hasLoadedOnce && !isLoading
}

fun resolveSpaceDynamicPresentationState(
    itemCount: Int,
    isLoading: Boolean,
    hasLoadedOnce: Boolean,
    lastLoadFailed: Boolean
): SpaceDynamicPresentationState {
    if (itemCount > 0) return SpaceDynamicPresentationState.CONTENT
    if (isLoading || !hasLoadedOnce) return SpaceDynamicPresentationState.LOADING
    if (lastLoadFailed) return SpaceDynamicPresentationState.ERROR
    return SpaceDynamicPresentationState.EMPTY
}

internal fun filterSpaceDynamicItemsByQuery(
    items: List<SpaceDynamicItem>,
    query: String
): List<SpaceDynamicItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return items

    return items.filter { item ->
        resolveSpaceDynamicSearchText(item).contains(normalizedQuery, ignoreCase = true)
    }
}

private fun resolveSpaceDynamicSearchText(item: SpaceDynamicItem): String {
    val content = item.modules.module_dynamic
    val major = content?.major
    return listOfNotNull(
        content?.desc?.text,
        major?.archive?.title,
        major?.archive?.desc,
        major?.opus?.title,
        major?.opus?.summary?.text
    )
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
}

internal fun resolveSpaceDynamicCardItems(items: List<SpaceDynamicItem>): List<DynamicItem> {
    return items.map(::resolveSpaceDynamicCardItem)
}

internal fun resolveSpaceDynamicCardItem(item: SpaceDynamicItem): DynamicItem {
    return DynamicItem(
        id_str = item.id_str,
        type = item.type,
        visible = item.visible,
        modules = DynamicModules(
            module_author = item.modules.module_author?.let { author ->
                DynamicAuthorModule(
                    mid = author.mid,
                    name = author.name,
                    face = author.face,
                    pub_time = author.pub_time,
                    pub_ts = author.pub_ts
                )
            },
            module_dynamic = item.modules.module_dynamic?.let { content ->
                DynamicContentModule(
                    desc = content.desc?.let { desc ->
                        DynamicDesc(
                            text = desc.text,
                            rich_text_nodes = desc.rich_text_nodes.map { node ->
                                RichTextNode(
                                    type = node.type,
                                    text = node.text,
                                    emoji = node.emoji?.let { emoji ->
                                        EmojiInfo(
                                            icon_url = emoji.icon_url,
                                            size = emoji.size,
                                            text = emoji.text
                                        )
                                    }
                                )
                            }
                        )
                    },
                    major = content.major?.let { major ->
                        DynamicMajor(
                            type = major.type,
                            archive = major.archive?.let { archive ->
                                ArchiveMajor(
                                    aid = archive.aid,
                                    bvid = archive.bvid,
                                    title = archive.title,
                                    cover = archive.cover,
                                    desc = archive.desc,
                                    duration_text = archive.duration_text,
                                    stat = ArchiveStat(
                                        play = archive.stat.play,
                                        danmaku = archive.stat.danmaku
                                    )
                                )
                            },
                            draw = major.draw?.let { draw ->
                                DrawMajor(
                                    id = draw.id,
                                    items = draw.items.map { drawItem ->
                                        DrawItem(
                                            src = drawItem.src,
                                            width = drawItem.width,
                                            height = drawItem.height
                                        )
                                    }
                                )
                            },
                            opus = major.opus?.let { opus ->
                                OpusMajor(
                                    summary = opus.summary?.let { summary ->
                                        OpusSummary(
                                            text = summary.text,
                                            rich_text_nodes = summary.rich_text_nodes.map { node ->
                                                RichTextNode(
                                                    type = node.type,
                                                    text = node.text,
                                                    emoji = node.emoji?.let { emoji ->
                                                        EmojiInfo(
                                                            icon_url = emoji.icon_url,
                                                            size = emoji.size,
                                                            text = emoji.text
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    },
                                    pics = opus.pics.map { pic ->
                                        OpusPic(
                                            url = pic.src,
                                            width = pic.width,
                                            height = pic.height
                                        )
                                    },
                                    title = opus.title
                                )
                            }
                        )
                    }
                )
            },
            module_stat = item.modules.module_stat?.let { stat ->
                DynamicStatModule(
                    comment = StatItem(
                        count = stat.comment.count,
                        forbidden = stat.comment.forbidden
                    ),
                    forward = StatItem(
                        count = stat.forward.count,
                        forbidden = stat.forward.forbidden
                    ),
                    like = StatItem(
                        count = stat.like.count,
                        forbidden = stat.like.forbidden
                    )
                )
            }
        )
    )
}
