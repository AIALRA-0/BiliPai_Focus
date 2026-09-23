package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppVersionPolicyTest {

    @Test
    fun appVersion_keepsFocusUpgradeVersionAboveExistingClients() {
        val buildFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()

        assertTrue(buildFile.contains("versionCode = 385"))
        assertTrue(buildFile.contains("versionName = \"9.1.1-focus.5\""))
        // Existing Focus clients compare this bridge version by versionName.
        assertTrue(!buildFile.contains("versionName = \"26."))
        assertTrue(!buildFile.contains("versionName = \"2026."))
    }
}
