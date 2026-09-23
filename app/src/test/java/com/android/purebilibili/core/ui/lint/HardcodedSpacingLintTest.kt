package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedSpacingLintTest {
    @Test
    fun migrated_features_use_app_spacing_tokens() {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex("""(?<![A-Za-z0-9_])\d+(?:\.\d+)?\.dp\b"""),
            allowlist = StyleLintAllowlist.SPACING_HITS,
        )
        assertTrue(
            offenders.isEmpty(),
            "Migrated feature UI contains literal layout spacing. Use AppSpacingTokens or a named Spec.\n" +
                offenders.joinToString("\n"),
        )
    }
}
