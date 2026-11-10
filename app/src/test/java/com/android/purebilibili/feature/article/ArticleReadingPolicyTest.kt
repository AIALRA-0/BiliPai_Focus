package com.android.purebilibili.feature.article

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArticleReadingPolicyTest {
    @Test
    fun readingAnchorSurvivesCoverAndCollectionHeaderChanges() {
        val saved = captureArticleReadingPosition(listIndex = 7, offset = 42, headerCount = 3, blockCount = 10)
        assertEquals(ArticleReadingPosition(4, 42), saved)
        assertEquals(5 to 42, resolveArticleReadingListPosition(requireNotNull(saved), headerCount = 1, blockCount = 10))
    }

    @Test
    fun shortenedOrEmptyBodyDoesNotRestoreIntoComments() {
        assertEquals(4 to 0, resolveArticleReadingListPosition(ArticleReadingPosition(9, 300), 2, 3))
        assertEquals(0 to 0, resolveArticleReadingListPosition(ArticleReadingPosition(9, 300), 2, 0))
        assertNull(captureArticleReadingPosition(listIndex = 5, offset = 30, headerCount = 2, blockCount = 3))
    }

    @Test
    fun titleAreaAndInvalidOffsetsRestoreSafely() {
        assertEquals(ArticleReadingPosition(), captureArticleReadingPosition(1, 100, 3, 10))
        assertEquals(4 to 0, resolveArticleReadingListPosition(ArticleReadingPosition(2, -100), 2, 10))
        assertEquals(0 to 0, resolveArticleReadingListPosition(ArticleReadingPosition(), 3, 10))
    }

    @Test
    fun invalidPreferenceValuesFallBackIndependently() {
        assertEquals(ArticleReadingPreferences(16, 115), normalizeArticleReadingPreferences(999, 115))
        assertEquals(ArticleReadingPreferences(20, 100), normalizeArticleReadingPreferences(20, -1))
    }

    @Test
    fun fontSizeAndLineSpacingScaleTogetherWithoutChangingDefaults() {
        val original = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        assertEquals(original, original.withArticleReadingPreferences(ArticleReadingPreferences()))
        val large = original.withArticleReadingPreferences(ArticleReadingPreferences(20, 130))
        assertEquals(20.sp, large.fontSize)
        assertEquals(39f, large.lineHeight.value, 0.001f)
    }

    @Test
    fun inlineExplicitSizesFollowReadingScaleOnlyOnce() {
        val annotated = buildArticleAnnotatedString(
            listOf(ArticleTextSpan("normal "), ArticleTextSpan("large", fontSizeSp = 24)),
            Color.Black, baseFontSize = 20.sp, explicitFontScale = 1.25f,
        )
        assertEquals(listOf(20.sp, 30.sp), annotated.spanStyles.map { it.item.fontSize })
    }
}
