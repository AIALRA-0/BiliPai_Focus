package com.android.purebilibili.feature.video.danmaku

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FaceOcclusionModelRemovalPolicyTest {

    @Test
    fun `video feature does not ship mlkit face model code`() {
        val buildFile = locateFile("app/build.gradle.kts", "build.gradle.kts")
        val videoSourceRoot = locateFile(
            "app/src/main/java/com/android/purebilibili/feature/video",
            "src/main/java/com/android/purebilibili/feature/video"
        )

        val offenders = mutableListOf<String>()
        val buildText = buildFile.readText()
        if (buildText.contains("play-services-mlkit-face-detection")) {
            offenders += "${buildFile.path}: play-services-mlkit-face-detection dependency is still present"
        }

        videoSourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if (
                        line.contains("com.google.mlkit") ||
                        line.contains("ModuleInstall") ||
                        line.contains("FaceDetection")
                    ) {
                        offenders += "${file.path}:${index + 1}: ${line.trim()}"
                    }
                }
            }

        assertTrue(
            offenders.isEmpty(),
            "ML Kit face model code should not be present:\n" + offenders.joinToString("\n")
        )
    }

    private fun locateFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Cannot locate any of: ${candidates.joinToString()}")
    }
}
