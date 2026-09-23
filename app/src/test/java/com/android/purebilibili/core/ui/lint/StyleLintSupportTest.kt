package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertEquals

class StyleLintSupportTest {

    @Test
    fun findMatchesIgnoresLineAndNestedBlockCommentExamples() {
        val source = """
            // RoundedCornerShape(4.dp)
            /* Outer comment
               /* RoundedCornerShape(8.dp) */
               RoundedCornerShape(10.dp)
            */
            val commentMarker = "/* not a comment */"
            val shape = RoundedCornerShape(12.dp)
        """.trimIndent()

        assertEquals(
            listOf("Example.kt:7: val shape = RoundedCornerShape(12.dp)"),
            StyleLintSupport.findMatches(
                source = source,
                relativePath = "Example.kt",
                pattern = Regex("RoundedCornerShape\\(\\d+\\.dp\\)"),
            ),
        )
    }
}
