package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicPublishComposerPolicyTest {

    @Test
    fun composerTokenInsertionKeepsReadableSpacing() {
        assertEquals("@用户 ", appendDynamicComposerToken("", "@用户 "))
        assertEquals("正文 @用户 ", appendDynamicComposerToken("正文", "@用户 "))
        assertEquals("正文 [doge]", appendDynamicComposerToken("正文 ", "[doge]"))
    }

    @Test
    fun liquidDockReuseFollowsTheUserSetting() {
        val source = loadComposerSource()

        assertTrue(source.contains("reuseEnabled = liquidGlassEnabled"))
        assertTrue(source.contains("shellLensIntensity = resolveFloatingDockGeometryScale("))
        assertTrue(source.contains("if (liquidGlassEnabled)"))
        assertTrue(source.contains("AppNativeSegmentedControl("))
    }

    @Test
    fun publishComposerCapturesBackdropFromFormSiblingNotChromeParent() {
        val source = loadComposerSource()
        val dialogText = source
            .substringAfter("text = {")
            .substringBefore("confirmButton = {")
        val outerColumnHeader = dialogText
            .substringAfter("Column(")
            .substringBefore("verticalArrangement")

        assertTrue(dialogText.contains("layerBackdrop(publishChromeBackdrop)"))
        assertTrue(dialogText.contains(".matchParentSize()\n                            .layerBackdrop(publishChromeBackdrop)"))
        assertTrue(outerColumnHeader.contains("verticalScroll(rememberScrollState())"))
        assertTrue(!outerColumnHeader.contains("layerBackdrop"))
        assertTrue(dialogText.contains("backdrop = publishChromeBackdrop"))
        assertTrue(dialogText.contains("miuixBackdrop = publishChromeBackdrop"))
        assertTrue(
            dialogText.indexOf("layerBackdrop(publishChromeBackdrop)") <
                dialogText.indexOf("backdrop = publishChromeBackdrop")
        )
    }

    private fun loadComposerSource(): String {
        val path = "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicPublishComposer.kt"
        return listOf(File(path), File("app/$path")).first { it.exists() }.readText()
    }
}
