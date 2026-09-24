package com.android.purebilibili.core.buildinfo

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 交付 APK 命名策略：源码结构测试（不依赖 Gradle 任务类加载）。
 * 规范：`BiliPai-Focus-<version>[-variant].apk`，禁止 `app-release.apk` 等默认名。
 */
class BiliPaiApkNamingPolicyTest {

    @Test
    fun buildScript_exportsCanonicalBiliPaiApkNames() {
        val source = loadAppBuildGradle()
        assertTrue(source.contains("archivesName.set(\"BiliPai-Focus-\$biliApkVersionName\")"))
        assertTrue(source.contains("\"BiliPai-Focus-\$biliApkVersionName.apk\""))
        assertTrue(source.contains("\"BiliPai-Focus-\$biliApkVersionName-\$variantName.apk\""))
        assertTrue(source.contains("outputs/bilipai/"))
        assertTrue(source.contains("export\${capitalizedVariantName}Apk"))
        assertTrue(source.contains("finalizedBy(exportTask)"))
        assertTrue(source.contains("never app-release"))
        assertTrue(source.contains("versionName = \"0.2.3-alpha.2.focus.1\""))
        assertTrue(source.contains("Delivery APK must be BiliPai-"))
    }

    @Test
    fun canonicalNames_matchExpectedPattern() {
        assertEquals(
            "BiliPai-Focus-0.2.3-alpha.2.focus.1.apk",
            resolveDeliveryNameFromScriptLogic(versionName = "0.2.3-alpha.2.focus.1", variantName = "release"),
        )
        assertEquals(
            "BiliPai-Focus-0.2.3-alpha.2.focus.1-dev.apk",
            resolveDeliveryNameFromScriptLogic(versionName = "0.2.3-alpha.2.focus.1", variantName = "dev"),
        )
    }

    @Test
    fun defaultAgpNames_areRejected() {
        assertFalse(isCanonicalNameFromScriptLogic("app-release.apk"))
        assertFalse(isCanonicalNameFromScriptLogic("app-dev.apk"))
        assertFalse(isCanonicalNameFromScriptLogic("app.apk"))
        assertFalse(isCanonicalNameFromScriptLogic("release.apk"))
        assertTrue(isCanonicalNameFromScriptLogic("BiliPai-Focus-0.2.3-alpha.2.focus.1.apk"))
        assertTrue(isCanonicalNameFromScriptLogic("BiliPai-Focus-0.2.3-alpha.2.focus.1-dev.apk"))
    }

    private fun resolveDeliveryNameFromScriptLogic(versionName: String, variantName: String): String {
        val safeVersion = versionName.trim().ifEmpty { "0" }
        val normalizedVariant = variantName.lowercase()
        return when (normalizedVariant) {
            "release" -> "BiliPai-Focus-$safeVersion.apk"
            else -> "BiliPai-Focus-$safeVersion-$normalizedVariant.apk"
        }
    }

    private fun isCanonicalNameFromScriptLogic(fileName: String): Boolean {
        val name = fileName.trim()
        if (!name.startsWith("BiliPai-") || !name.endsWith(".apk", ignoreCase = true)) return false
        val lower = name.lowercase()
        if (lower.startsWith("app-") || lower.contains("app-release") || lower.contains("app-dev")) {
            return false
        }
        if (Regex("""^app[-_.]""", RegexOption.IGNORE_CASE).containsMatchIn(name)) return false
        return true
    }

    private fun loadAppBuildGradle(): String {
        val file = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts"),
        ).firstOrNull { it.exists() } ?: error("app/build.gradle.kts not found")
        return file.readText()
    }
}
