package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApkArtifactNamingPolicyTest {

    @Test
    fun gradleExportsCanonicalReleaseAndDevApkNames() {
        val buildFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { file -> file.exists() }.readText()

        assertTrue(buildFile.contains("abstract class ExportBiliPaiApkTask"))
        assertTrue(buildFile.contains("outputFileName.set(deliveryFileName)"))
        assertTrue(buildFile.contains("\"BiliPai-Focus-\$biliApkVersionName.apk\""))
        assertTrue(buildFile.contains("\"BiliPai-Focus-\$biliApkVersionName-\$variantName.apk\""))
        assertTrue(buildFile.contains("outputs/bilipai/\$variantName"))
        assertTrue(buildFile.contains("variantName == \"release\" || variantName == \"dev\""))
        assertFalse(buildFile.contains("tasks.withType(com.android.build.gradle.tasks.PackageApplication"))
    }

    @Test
    fun workflowCollectsOnlyCanonicalBiliPaiArtifacts() {
        val workflow = listOf(
            File(".github/workflows/Build.yml"),
            File("../.github/workflows/Build.yml")
        ).first { file -> file.exists() }.readText()

        assertTrue(workflow.contains("assembleDev"))
        assertTrue(workflow.contains("outputs/bilipai/\${{ steps.set_build_type.outputs.build_type }}"))
        assertTrue(workflow.contains("-name \"BiliPai-*.apk\""))
        assertFalse(workflow.contains("find \${{ github.workspace }}/app/build/outputs/apk/"))
    }
}
