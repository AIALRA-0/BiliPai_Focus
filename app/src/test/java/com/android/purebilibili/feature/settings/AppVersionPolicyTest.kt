package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertEquals(392, versionCode)
        assertTrue(defaultConfig.contains("versionName = \"0.7.9-beta.9.focus.2\""))
        assertTrue(
            buildFile.contains("语义化") ||
                buildFile.contains("MAJOR.MINOR.PATCH") ||
                buildFile.contains("X.Y.Z")
        )
        assertTrue(!buildFile.contains("versionName = \"26."))
        assertTrue(!buildFile.contains("versionName = \"2026."))
    }
}
