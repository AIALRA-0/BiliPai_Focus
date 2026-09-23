package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedShapeLintTest {

    @Test
    fun feature_kt_must_not_introduce_new_hardcoded_RoundedCornerShape() {
        val legacyOffenders = StyleLintSupport.findOffenders(
            pattern = Regex("""RoundedCornerShape\(\s*\d+"""),
            allowlist = StyleLintAllowlist.SHAPE_HITS
        )
        val migratedOffenders = StyleLintSupport.findOffendersInMigratedFeatures(
            Regex(
                """RoundedCornerShape\s*\([^)]*\d+(?:\.\d+)?\s*\.dp""",
                RegexOption.DOT_MATCHES_ALL,
            ),
            allowlist = StyleLintAllowlist.SHAPE_HITS,
        )
        val offenders = (legacyOffenders + migratedOffenders).distinct()
        assertTrue(
            offenders.isEmpty(),
            "New hardcoded RoundedCornerShape detected in feature/. Use " +
                "AppShapes.container(level) or MaterialTheme.shapes.* instead, " +
                "or add the file to StyleLintAllowlist.SHAPE_HITS with reason.\n" +
                offenders.joinToString("\n")
        )
    }
}
