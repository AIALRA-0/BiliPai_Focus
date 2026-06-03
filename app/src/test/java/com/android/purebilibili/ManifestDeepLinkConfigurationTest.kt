package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ManifestDeepLinkConfigurationTest {

    @Test
    fun manifest_supports_bilibili_scheme_open_with() {
        val manifest = loadManifestText()

        assertTrue(
            manifest.contains("""android:scheme="bilibili""""),
            "AndroidManifest should declare bilibili:// VIEW intent-filter so BiliPai appears in Open With"
        )
        assertTrue(
            manifest.contains("""android:scheme="bili""""),
            "AndroidManifest should declare bili:// VIEW intent-filter for broader deep link compatibility"
        )
        assertTrue(
            manifest.contains("""android.intent.category.BROWSABLE"""),
            "AndroidManifest deep links should be browsable from external apps"
        )
    }

    @Test
    fun manifest_declares_share_package_visibility_queries() {
        val manifest = loadManifestText()

        assertTrue(
            manifest.contains("<queries>"),
            "AndroidManifest should declare Android 11+ package visibility queries"
        )
        assertTrue(
            manifest.contains("""android:name="com.tencent.mm""""),
            "AndroidManifest should query WeChat for targeted sharing"
        )
        assertTrue(
            manifest.contains("""android:name="com.tencent.mobileqq""""),
            "AndroidManifest should query QQ for targeted sharing"
        )
    }

    private fun loadManifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate AndroidManifest.xml from ${File(".").absolutePath}")
        return manifestFile.readText()
    }
}
