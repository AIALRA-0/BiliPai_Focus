package com.android.purebilibili.feature.profile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileServicesVisibilityPolicyTest {

    @Test
    fun `history service hides when history is already visible in bottom bar`() {
        assertFalse(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
            )
        )
    }

    @Test
    fun `history service stays visible when history is not in bottom bar`() {
        assertTrue(
            shouldShowProfileHistoryService(
                bottomBarVisibleTabIds = listOf("HOME", "DYNAMIC", "PROFILE")
            )
        )
    }

    @Test
    fun `immersive mobile services use compact island and separate account actions`() {
        val source = File("src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt").readText()
        val servicesSource = source.substringAfter("fun ServicesSection(")
            .substringBefore("@Composable\nprivate fun ProfileFavoriteFolderShortcutGrid")

        assertTrue(servicesSource.contains("val useImmersiveServiceLayout = borderColor != null"))
        assertTrue(servicesSource.contains("ProfileServicesListIsland("))
        assertTrue(servicesSource.contains("ProfileServiceRow("))
        assertTrue(servicesSource.contains("ProfileAccountActionArea("))
        assertTrue(servicesSource.contains("compactHorizontal = true"))
        assertTrue(servicesSource.contains("onMoreClick = onFavoriteClick"))
        assertFalse(servicesSource.contains("ProfileFloatingServiceItem("))
        val immersiveBranchSource = servicesSource.substringAfter("if (useImmersiveServiceLayout) {")
            .substringBefore("} else {")
        assertFalse(immersiveBranchSource.contains("ProfileFloatingServiceItem("))
    }

    @Test
    fun `tablet profile forwards inbox click and keeps service pane scrollable`() {
        val source = File("src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt").readText()
        val tabletProfileSignature = extractFunctionSignature(source, "TabletProfileContent")
        val tabletProfileBody = source.substringAfter("fun TabletProfileContent(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)")
        val tabletProfileCallArea = source.substringBefore("fun TabletProfileContent(")

        assertTrue(
            "TabletProfileContent 必须显式接收消息中心点击回调",
            tabletProfileSignature.contains("onInboxClick: () -> Unit")
        )
        assertTrue(
            "ProfileScreen 平板分支必须把消息中心导航回调传给 TabletProfileContent",
            tabletProfileCallArea.contains("onInboxClick = onInboxClick")
        )
        assertTrue(
            "TabletProfileContent 必须把消息中心点击回调传给 ServicesSection",
            tabletProfileBody.contains("onInboxClick = onInboxClick")
        )
        assertTrue(
            "平板右侧服务区必须支持纵向滚动，避免窗口较矮时内容不可达",
            tabletProfileBody.contains(".verticalScroll(rememberScrollState())")
        )
    }

    @Test
    fun `profile dynamic menu exposes delete and image preview keeps source rect animation`() {
        val source = File("src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt").readText()
        val cardSource = source.substringAfter("private fun ProfileDynamicCard(")
            .substringBefore("@Composable\nprivate fun ProfileDynamicOriginalContent(")
        val mediaSource = source.substringAfter("private fun ProfileDynamicMajorContent(")
            .substringBefore("@Composable\nprivate fun ProfileDynamicActionRow(")

        assertTrue(cardSource.contains("val deleteAction = remember(item) { resolveProfileDynamicDeleteAction(item) }"))
        assertTrue(cardSource.contains("pendingDeleteAction = deleteAction"))
        assertTrue(cardSource.contains("onDeleteClick(action)"))
        assertTrue(mediaSource.contains("var sourceRect by remember(item.id_str, imageUrls)"))
        assertTrue(mediaSource.contains(".onGloballyPositioned { coordinates ->"))
        assertTrue(mediaSource.contains("sourceRect = coordinates.boundsInWindow()"))
        assertTrue(mediaSource.contains("sourceRect = sourceRect"))
        assertTrue(mediaSource.contains("sourceCornerRadiusDp = 6f"))
    }

    private fun extractFunctionSignature(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName(")
        require(start >= 0) { "Cannot find function $functionName" }
        val openParen = source.indexOf('(', start)
        val closeParen = findMatching(source, openParen, '(', ')')
        return source.substring(start, closeParen + 1)
    }

    private fun findMatching(source: String, openIndex: Int, openChar: Char, closeChar: Char): Int {
        require(openIndex >= 0 && source[openIndex] == openChar) { "Invalid open index" }
        var depth = 0
        for (index in openIndex until source.length) {
            when (source[index]) {
                openChar -> depth++
                closeChar -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        error("No matching $closeChar")
    }
}
