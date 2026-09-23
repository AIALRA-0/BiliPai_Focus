package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedMotionLintTest {

    @Test
    fun feature_kt_must_not_introduce_new_literal_tween_or_spring_durations() {
        val legacyOffenders = StyleLintSupport.findOffenders(
            pattern = Regex("""\b(tween|spring)\s*\(\s*\d+"""),
            allowlist = StyleLintAllowlist.MOTION_HITS
        )
        val migratedOffenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex(
                """\b(?:tween|spring)\s*\([^)]*\b(?:durationMillis|dampingRatio|stiffness)?""" +
                    """(?:\s*=\s*)?\d+(?:\.\d+)?f?""",
                RegexOption.DOT_MATCHES_ALL,
            ),
        )
        val offenders = (legacyOffenders + migratedOffenders).distinct()
        assertTrue(
            offenders.isEmpty(),
            "New literal tween(N)/spring(N) detected in feature/. Use " +
                "AppMotionTokens.standardSpec() / emphasizedSpec() / expressiveSpec() " +
                "instead, or add the file to StyleLintAllowlist.MOTION_HITS with reason.\n" +
                offenders.joinToString("\n")
        )
    }
}
