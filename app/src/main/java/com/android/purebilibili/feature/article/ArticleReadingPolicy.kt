package com.android.purebilibili.feature.article

import androidx.compose.ui.text.TextStyle

internal data class ArticleReadingPreferences(
    val fontSizeSp: Int = 16,
    val lineSpacingPercent: Int = 100,
) {
    val fontScale: Float get() = fontSizeSp / 16f
    val lineScale: Float get() = lineSpacingPercent / 100f
}

internal val articleFontSizePresets = listOf(14, 16, 18, 20)
internal val articleLineSpacingPresets = listOf(100, 115, 130)

internal fun TextStyle.withArticleReadingPreferences(preferences: ArticleReadingPreferences): TextStyle = copy(
    fontSize = fontSize * preferences.fontScale,
    lineHeight = lineHeight * preferences.fontScale * preferences.lineScale,
)

internal fun normalizeArticleReadingPreferences(fontSize: Int, lineSpacing: Int) = ArticleReadingPreferences(
    fontSize.takeIf { it in articleFontSizePresets } ?: 16,
    lineSpacing.takeIf { it in articleLineSpacingPresets } ?: 100,
)

// blockIndex == -1 表示文章开头；评论区不写入正文阅读记录。
internal data class ArticleReadingPosition(val blockIndex: Int = -1, val offset: Int = 0)

internal fun resolveArticleReadingListPosition(
    position: ArticleReadingPosition,
    headerCount: Int,
    blockCount: Int,
): Pair<Int, Int> {
    if (position.blockIndex < 0 || blockCount <= 0) return 0 to 0
    val block = position.blockIndex.coerceAtMost(blockCount - 1)
    return (headerCount + block) to if (block == position.blockIndex) position.offset.coerceAtLeast(0) else 0
}

internal fun captureArticleReadingPosition(
    listIndex: Int,
    offset: Int,
    headerCount: Int,
    blockCount: Int,
): ArticleReadingPosition? = when {
    listIndex < headerCount -> ArticleReadingPosition()
    listIndex >= headerCount + blockCount -> null
    else -> ArticleReadingPosition(listIndex - headerCount, offset.coerceAtLeast(0))
}
