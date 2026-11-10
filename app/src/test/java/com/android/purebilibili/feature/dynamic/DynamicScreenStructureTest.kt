package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicScreenStructureTest {

    @Test
    fun `dynamic staggered grid provides stable content types`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val gridSource = source
            .substringAfter("FeedVerticalStaggeredGrid(")
            .substringBefore("@Composable\nprivate fun OldContentDivider")

        assertTrue(gridSource.contains("contentType = \"dynamic_empty_state\""))
        assertTrue(gridSource.contains("contentType = { \"dynamic_card\" }"))
        assertTrue(gridSource.contains("contentType = \"dynamic_old_content_divider\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_loading_footer\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_no_more_footer\""))
        assertTrue(source.contains("visibleItemsInfo.maxOfOrNull { it.index }"))
        assertTrue(source.contains("shouldLoadMoreDynamicFeed("))
    }

    @Test
    fun `report dialog and additional cards use shared native components`() {
        val screenSource = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        ).readText()
        val sidebarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt"
        ).readText()

        assertTrue(screenSource.contains("AppListItem("))
        assertTrue(screenSource.contains("AppRadioButton("))
        assertTrue(cardSource.contains("AppContentCard("))
        assertTrue(sidebarSource.contains("AppTextButton("))
    }

    @Test
    fun `fold bar unfolds related dynamics in place instead of opening detail`() {
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        ).readText()
        val foldBlock = cardSource
            .substringAfter("PiliPlus: fold InkWell")
            .substringBefore("if (!isDetail) {\n            AppHorizontalDivider")

        assertTrue(screenSource.contains("onUnfoldRelatedClick = unfoldRelatedDynamics"))
        assertTrue(foldBlock.contains("onUnfoldRelatedClick?.invoke(item.id_str)"))
        assertFalse(foldBlock.contains("openDynamicDetail"))
    }
}
