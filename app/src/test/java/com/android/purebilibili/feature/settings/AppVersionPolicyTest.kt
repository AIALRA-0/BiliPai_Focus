package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppVersionPolicyTest {

    @Test
    fun appVersion_keepsTheUpstreamNameEpochAndRaisesFocusVersionCode() {
        val buildFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()
        val defaultConfig = buildFile.substringAfter("defaultConfig {").substringBefore("}")
        val versionCode = Regex("versionCode\\s*=\\s*(\\d+)")
            .find(defaultConfig)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        assertTrue((versionCode ?: 0) > 389)
        assertTrue(defaultConfig.contains("versionName = \"0.7.9-beta.7.focus.1\""))
        assertTrue(
            buildFile.contains("语义化") ||
                buildFile.contains("MAJOR.MINOR.PATCH") ||
                buildFile.contains("X.Y.Z")
        )
        assertTrue(!buildFile.contains("versionName = \"26."))
        assertTrue(!buildFile.contains("versionName = \"2026."))
    }
}
