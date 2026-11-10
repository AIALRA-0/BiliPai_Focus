package com.android.purebilibili.feature.live

import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.LiveEmoticonPackage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDraftPreviewPolicyTest {
    @Test
    fun onlyTextSendableEmotesWithResourcesAppearInPreview() {
        val resources = liveDraftEmoteResources(listOf(LiveEmoticonPackage(
            id = 1,
            name = "房间表情",
            items = listOf(
                LiveEmoticonItem("[笑]", "https://example.test/smile.png", dmType = 0, width = 48, height = 24),
                LiveEmoticonItem("大表情", "https://example.test/large.png", dmType = 1),
                LiveEmoticonItem("[空]", "", dmType = 0),
            ),
        )))
        assertEquals(setOf("[笑]"), resources.keys)
        assertEquals(48, resources.getValue("[笑]").width)
        assertEquals(listOf("前" to false, "[笑]" to true, "后[未知]" to false),
            liveDraftPreviewSegments("前[笑]后[未知]", resources))
    }

    @Test
    fun roomChangesRemoveResourcesFromPreviousRoom() {
        val packages = listOf(LiveEmoticonPackage(1, "房间 A", listOf(
            LiveEmoticonItem("[房间A]", "https://example.test/a.png", dmType = 0),
        )))
        assertTrue(liveDraftPreviewSegments("[房间A]", liveDraftEmoteResources(packages)).single().second)
        assertFalse(liveDraftPreviewSegments("[房间A]", liveDraftEmoteResources(emptyList())).single().second)
    }
}
