package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveTextFieldStructureTest {

    @Test
    fun miuixTextFieldUsesTheTextInputPrimitiveInsteadOfSearchInput() {
        val source = loadSource()
        val miuixBranch = source
            .substringAfter("fun AdaptiveTextFieldRenderer(")
            .substringAfter("if (shouldUseNativeMiuixSearchBar(uiStyle)) {")
            .substringBefore("    OutlinedTextField(")

        assertTrue(miuixBranch.contains("MiuixTextField("))
        assertTrue(miuixBranch.contains("useLabelAsPlaceholder = label == null"))
        assertFalse(miuixBranch.contains("InputField("))
    }

    private fun loadSource(): String {
        val path = "design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt"
        return listOf(
            File(path),
            File("../$path"),
        ).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate AdaptivePreferenceComponents.kt from ${File(".").absolutePath}")
    }
}
