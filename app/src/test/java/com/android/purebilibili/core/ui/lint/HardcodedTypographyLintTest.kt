package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedTypographyLintTest {
    @Test
    fun migrated_features_use_theme_typography_or_named_visual_policy() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex("""(?<![A-Za-z0-9_])\d+(?:\.\d+)?\.sp\b"""),
            allowlist = StyleLintAllowlist.TYPOGRAPHY_HITS,
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains literal typography. Use MaterialTheme.typography " +
                "or a named VisualPolicy for media-density exceptions.\n" + offenders.joinToString("\n"),
        )
    }
}
