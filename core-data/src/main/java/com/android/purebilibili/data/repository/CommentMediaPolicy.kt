package com.android.purebilibili.data.repository

import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyPicture

/**
 * 评论正文表情与图片的展示规则。原实现位于手机端 ReplyComponents.kt
 * （EMOTE_TOKEN_PATTERN、collectRenderableEmoteKeys、CommentPictures 的
 * URL 归一化、单图比例与多图列数），TV 评论阅读消费同一规则，故下沉共享；
 * 手机端原入口保留并委托。
 */

/** 评论正文中的表情占位 token，如「[doge]」。原手机端 EMOTE_TOKEN_PATTERN 语义保持不变。 */
val COMMENT_EMOTE_TOKEN_PATTERN = """\[(.*?)\]""".toRegex()

/** 楼中楼与主评论共用的表情行高占位（相对正文字号的 em 数），与手机端一致。 */
const val COMMENT_EMOTE_INLINE_EM = 1.4f

/** 单条评论最多展示的图片数量，超出部分不渲染，与手机端一致。 */
const val COMMENT_PICTURE_MAX_COUNT = 9

/** 评论正文中可内联渲染的表情集合：token 必须出现在服务端 emote 表中。 */
fun resolveCommentRenderableEmoteKeys(
    text: String,
    emoteUrls: Map<String, String>
): Set<String> {
    if (text.isEmpty() || emoteUrls.isEmpty()) return emptySet()
    return COMMENT_EMOTE_TOKEN_PATTERN.findAll(text)
        .map { it.value }
        .filter { emoteUrls.containsKey(it) }
        .toSet()
}

/** 评论正文的顺序分段：表情 token 命中 emote 表时为表情段，其余为文本段。 */
sealed class CommentEmoteSegment {
    data class Text(val value: String) : CommentEmoteSegment()
    data class Emote(val token: String, val url: String) : CommentEmoteSegment()
}

fun resolveCommentEmoteSegments(
    text: String,
    emoteUrls: Map<String, String>
): List<CommentEmoteSegment> {
    if (text.isEmpty()) return emptyList()
    if (emoteUrls.isEmpty()) return listOf(CommentEmoteSegment.Text(text))
    val segments = mutableListOf<CommentEmoteSegment>()
    var lastIndex = 0
    COMMENT_EMOTE_TOKEN_PATTERN.findAll(text).forEach { match ->
        val url = emoteUrls[match.value] ?: return@forEach
        if (lastIndex < match.range.first) {
            segments.add(CommentEmoteSegment.Text(text.substring(lastIndex, match.range.first)))
        }
        segments.add(CommentEmoteSegment.Emote(match.value, url))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        segments.add(CommentEmoteSegment.Text(text.substring(lastIndex)))
    }
    return segments
}

/** 评论图片的展示 URL：修复协议、去掉尺寸参数；顺序与入参一致。 */
fun resolveCommentPictureUrls(pictures: List<ReplyPicture>): List<String> {
    return pictures.map { FormatUtils.normalizeImageUrl(it.imgSrc) }.filter { it.isNotEmpty() }
}

/**
 * 单张图片的显示宽高比：来自服务端宽高并限制在 0.5–2 之间，未知时回退 4:3，
 * 与手机端 CommentPictures 一致。
 */
fun resolveCommentSinglePictureAspectRatio(picture: ReplyPicture): Float {
    return if (picture.imgHeight > 0 && picture.imgWidth > 0) {
        (picture.imgWidth.toFloat() / picture.imgHeight.toFloat()).coerceIn(0.5f, 2f)
    } else {
        4f / 3f
    }
}

/** 多张图片的网格列数：不超过 4 张时两列，否则三列，与手机端一致。 */
fun resolveCommentPictureGridColumns(displayCount: Int): Int {
    return if (displayCount <= 4) 2 else 3
}
