package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class MigratedFeatureStyleLintTest {
    @Test
    fun migrated_features_haveNoLiteralShapes() {
        assertNoOffenders(
            Regex(
                """RoundedCornerShape\s*\([^)]*\d+(?:\.\d+)?\s*\.dp""",
                RegexOption.DOT_MATCHES_ALL,
            ),
            "迁移模块仍有字面圆角",
            allowlist = StyleLintAllowlist.SHAPE_HITS,
        )
    }

    @Test
    fun migrated_features_haveNoLiteralMotionParameters() {
        assertNoOffenders(
            Regex(
                """\b(?:tween|spring)\s*\([^)]*\b(?:durationMillis|dampingRatio|stiffness)?""" +
                    """(?:\s*=\s*)?\d+(?:\.\d+)?f?""",
                RegexOption.DOT_MATCHES_ALL,
            ),
            "迁移模块仍有字面动效参数",
        )
    }

    @Test
    fun migrated_features_haveNoDirectBaseSurfaceReads() {
        assertNoOffenders(
            Regex("""MaterialTheme\.colorScheme\.(surface|background)\b"""),
            "迁移模块仍直接读取基础 surface/background",
        )
    }

    private fun assertNoOffenders(
        pattern: Regex,
        message: String,
        allowlist: Set<String> = emptySet(),
    ) {
        val offenders = StyleLintSupport.findOffendersInMigratedFeatures(pattern, allowlist)
        assertTrue(offenders.isEmpty(), "$message\n${offenders.joinToString("\n")}")
    }
}
