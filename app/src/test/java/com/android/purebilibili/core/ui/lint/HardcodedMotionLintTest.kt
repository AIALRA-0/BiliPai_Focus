package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedMotionLintTest {

    @Test
    fun feature_kt_must_not_introduce_new_literal_tween_or_spring_durations() {
        // Float-named spring params (dampingRatio, stiffness) are not matched
        // because the first positional arg here must be a digit.
        val pattern = Regex("""\b(tween|spring)\s*\(\s*\d+""")
        val offenders = StyleLintSupport.findOffenders(
            pattern = pattern,
            allowlist = StyleLintAllowlist.MOTION_HITS
        )
        assertTrue(
            offenders.isEmpty(),
            "New literal tween(N)/spring(N) detected in feature/. Use " +
                "AppMotionTokens.standardSpec() / emphasizedSpec() / expressiveSpec() " +
                "instead, or add the file to StyleLintAllowlist.MOTION_HITS with reason.\n" +
                offenders.joinToString("\n")
        )
    }
}
