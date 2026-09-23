package com.android.purebilibili.feature.dynamic.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicCardR8SafetyPolicyTest {

    @Test
    fun dynamicCardEntryPoint_keepsComposeSignatureGrouped() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        )
        val signature = source
            .substringAfter("fun DynamicCardV2(")
            .substringBefore(") {")

        assertTrue(signature.contains("actions: DynamicCardActions"))
        assertTrue(signature.contains("presentation: DynamicCardPresentation"))
        assertFalse(signature.contains("onVideoClick:"))
        assertFalse(signature.contains("onLikeClickWithState:"))
        assertTrue(
            "DynamicCardV2 must keep a small Compose/R8 entry signature",
            signature.lineSequence().count { ':' in it } <= 4,
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
