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
    fun editingTextMovesIntactTokensAndDropsChangedTokens() {
        val token = com.android.purebilibili.data.model.response.DynamicPublishToken(3, 6, 2, "42", "@甲乙")
        assertEquals(listOf(token.copy(start = 5, end = 8)), updateDynamicComposerTokens("正文 @甲乙", "新 正文 @甲乙", listOf(token)))
        assertEquals(emptyList(), updateDynamicComposerTokens("正文 @甲乙", "正文 @甲", listOf(token)))
        assertEquals(listOf(token.copy(start = 6, end = 9)), replaceDynamicComposerTokens(listOf(token), 3, 3, 3))
    }

    @Test
    fun unresolvedTextMovesWithEditsAndClearsOnlyWhenReplacedOrRemoved() {
        val issue = com.android.purebilibili.data.model.response.DynamicRichTextIssue(3, 6)
        assertEquals(listOf(issue.copy(start = 5, end = 8)), updateDynamicRichTextIssues("正文 @甲乙", "新 正文 @甲乙", listOf(issue)))
        assertEquals(listOf(issue.copy(end = 5)), updateDynamicRichTextIssues("正文 @甲乙", "正文 @甲", listOf(issue)))
        assertTrue(replaceDynamicRichTextIssues(listOf(issue), 3, 6, 4).isEmpty())
        assertTrue(updateDynamicRichTextIssues("正文 @甲乙", "正文 ", listOf(issue)).isEmpty())
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
        assertTrue(dialogText.contains(".matchParentSize()"))
        assertTrue(dialogText.contains("if (publishChromeBackdrop != null)"))
        assertTrue(dialogText.contains("Modifier.layerBackdrop(publishChromeBackdrop)"))
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
