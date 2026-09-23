package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val KOTLIN_DIGITS = """\d(?:_?\d)*"""
private const val KOTLIN_FLOAT_LITERAL =
    """-?(?:(?:$KOTLIN_DIGITS)(?:\.(?:$KOTLIN_DIGITS)?)?|\.(?:$KOTLIN_DIGITS))[fF]"""
private const val KOTLIN_INTEGER_LITERAL =
    """-?(?:0[xX][0-9A-Fa-f](?:_?[0-9A-Fa-f])*|0[bB][01](?:_?[01])*|$KOTLIN_DIGITS)[uU]?[lL]?"""

internal val HardcodedColorPattern = Regex(
    """(?<![A-Za-z0-9_])Color\s*\(\s*(?:""" +
        """(?:$KOTLIN_INTEGER_LITERAL)""" +
        """|(?:$KOTLIN_INTEGER_LITERAL\s*,\s*){2,3}$KOTLIN_INTEGER_LITERAL""" +
        """|(?:$KOTLIN_FLOAT_LITERAL\s*,\s*){2,3}$KOTLIN_FLOAT_LITERAL""" +
        """|[^)]*(?:red|green|blue|alpha)\s*=\s*-?(?:(?:$KOTLIN_DIGITS)(?:\.(?:$KOTLIN_DIGITS)?)?|\.(?:$KOTLIN_DIGITS))[fF]?[^)]*""" +
        """)\s*\)""" +
        """|Color\.(?:Black|White|Red|Blue|Green|Yellow|Gray|LightGray|DarkGray)""",
    setOf(RegexOption.DOT_MATCHES_ALL),
)

class HardcodedColorLintTest {
    @Test
    fun migrated_features_use_theme_roles_or_named_palette() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            HardcodedColorPattern,
            allowlist = StyleLintAllowlist.COLOR_HITS,
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains raw colors. Use theme roles or a named Palette/Policy.\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun scanner_covers_integer_float_and_named_literal_overloads() {
        listOf(
            "Color(0xFFFF0000)",
            "Color(16711680)",
            "Color(16_711_680)",
            "Color(0xFF_FF0000)",
            "Color(0b1111_0000)",
            "Color(255, 0, 0)",
            "Color(255, 0, 0, 128)",
            "Color(1f, 0f, 0f)",
            "Color(1F, 0F, 0F)",
            "Color(.5f, 0.25f, 0f, 1f)",
            "Color(red = 1f, green = 0f, blue = 0f)",
            "Color.Red",
        ).forEach { source ->
            assertTrue(HardcodedColorPattern.containsMatchIn(source), "未识别 $source")
        }
    }

    @Test
    fun scanner_ignores_runtime_values_and_named_semantic_colors() {
        listOf(
            "Color(colorInt)",
            "Color(argb.toLong(16))",
            "Color.Transparent",
            "MaterialTheme.colorScheme.primary",
            "LiveStatusPalette.LevelHigh",
        ).forEach { source ->
            assertFalse(HardcodedColorPattern.containsMatchIn(source), "误报 $source")
        }
    }
}
