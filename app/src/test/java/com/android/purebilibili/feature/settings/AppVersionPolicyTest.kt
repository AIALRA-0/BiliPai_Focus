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

        assertTrue(buildFile.contains("versionCode = 388"))
        assertTrue(buildFile.contains("versionName = \"9.1.1-focus.5\""))
        // Keep Focus release identity and advance beyond both existing Focus and upstream codes.
        assertTrue(
            buildFile.contains("语义化") ||
                buildFile.contains("MAJOR.MINOR.PATCH") ||
                buildFile.contains("X.Y.Z")
        )
        assertTrue(!buildFile.contains("versionName = \"26."))
        assertTrue(!buildFile.contains("versionName = \"2026."))
    }
}
