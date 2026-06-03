package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedSurfaceLintTest {

    @Test
    fun feature_kt_must_not_read_MaterialTheme_colorScheme_surface_or_background_directly() {
        val pattern = Regex("""MaterialTheme\.colorScheme\.(surface|background)\b""")
        val offenders = StyleLintSupport.findOffenders(
            pattern = pattern,
            allowlist = StyleLintAllowlist.SURFACE_HITS
        )
        assertTrue(
            offenders.isEmpty(),
            "New direct MaterialTheme.colorScheme.(surface|background) read in feature/. " +
                "Use AppSurfaceTokens.cardContainer() / groupedListContainer() / " +
                "chromeBackground() instead, or add the file to " +
                "StyleLintAllowlist.SURFACE_HITS with reason.\n" +
                offenders.joinToString("\n")
        )
    }
}
