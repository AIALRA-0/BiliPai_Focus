package com.android.purebilibili.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

/** Opt-in subtree scope; ordinary app text keeps its existing rendering. */
val LocalAppTextSearchQuery = compositionLocalOf { "" }

private fun isSearchSeparator(char: Char): Boolean = char.isWhitespace() || char in "/&+-_:：·()（）"

internal fun appTextSearchMatchRanges(text: String, query: String): List<IntRange> {
    val needle = query.filterNot(::isSearchSeparator).lowercase()
    if (needle.isEmpty()) return emptyList()
    val positions = text.indices.filter { !isSearchSeparator(text[it]) }
    val normalizedText = positions.map { text[it].lowercaseChar() }.joinToString("")
    val ranges = mutableListOf<IntRange>()
    var offset = 0
    while (offset <= normalizedText.length - needle.length) {
        val index = normalizedText.indexOf(needle, offset)
        if (index < 0) break
        ranges += positions[index]..positions[index + needle.length - 1]
        offset = index + needle.length
    }
    return ranges
}

@Composable
internal fun hasAppTextSearchMatch(title: String, subtitle: String?): Boolean {
    val query = LocalAppTextSearchQuery.current
    return appTextSearchMatchRanges(title, query).isNotEmpty() ||
        appTextSearchMatchRanges(subtitle.orEmpty(), query).isNotEmpty()
}

@Composable
fun appSearchHighlightedText(text: String): AnnotatedString {
    val ranges = appTextSearchMatchRanges(text, LocalAppTextSearchQuery.current)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val colors = MaterialTheme.colorScheme
    return AnnotatedString.Builder(text).apply {
        ranges.forEach { range ->
            addStyle(
                SpanStyle(background = colors.primaryContainer, color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold),
                range.first,
                range.last + 1,
            )
        }
    }.toAnnotatedString()
}
