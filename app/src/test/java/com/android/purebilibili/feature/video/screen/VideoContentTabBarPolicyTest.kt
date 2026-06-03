package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoContentTabBarPolicyTest {

    @Test
    fun `tab bar layout reserves trailing danmaku action area`() {
        val spec = resolveVideoContentTabBarLayoutSpec(widthDp = 412)

        assertEquals(1f, spec.tabsRowWeight)
        assertTrue(spec.tabsRowScrollable)
        assertEquals(12, spec.containerHorizontalPaddingDp)
        assertEquals(12, spec.tabHorizontalPaddingDp)
        assertEquals(44, spec.segmentedControlHeightDp)
        assertEquals(30, spec.segmentedControlIndicatorHeightDp)
        assertTrue(
            hasVideoContentTabBarIndicatorScaleClearance(
                containerHeightDp = spec.segmentedControlHeightDp,
                indicatorHeightDp = spec.segmentedControlIndicatorHeightDp
            )
        )
    }

    @Test
    fun `danmaku input stays visible when player is expanded`() {
        assertTrue(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = false
            )
        )
    }

    @Test
    fun `danmaku input hidden when player is collapsed`() {
        assertFalse(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = true
            )
        )
    }

    @Test
    fun `danmaku action layout keeps settings target comfortably tappable`() {
        val policy = resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp = 412)

        assertEquals("发弹幕", policy.sendLabel)
        assertEquals(40, policy.secondaryControlHeightDp)
        assertEquals(20, policy.secondaryControlCornerRadiusDp)
        assertEquals(40, policy.settingsButtonSizeDp)
        assertEquals(20, policy.settingsIconSizeDp)
    }

    @Test
    fun `compact phone layout tightens tabs and danmaku actions`() {
        val spec = resolveVideoContentTabBarLayoutSpec(widthDp = 393)
        val policy = resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp = 393)

        assertEquals(8, spec.containerHorizontalPaddingDp)
        assertEquals(8, spec.tabHorizontalPaddingDp)
        assertEquals(10, spec.tabSpacingDp)
        assertEquals(16, spec.selectedTabFontSizeSp)
        assertEquals(44, spec.segmentedControlHeightDp)
        assertTrue(
            hasVideoContentTabBarIndicatorScaleClearance(
                containerHeightDp = spec.segmentedControlHeightDp,
                indicatorHeightDp = spec.segmentedControlIndicatorHeightDp
            )
        )
        assertEquals("发弹幕", policy.sendLabel)
        assertEquals(40, policy.secondaryControlHeightDp)
        assertEquals(20, policy.secondaryControlCornerRadiusDp)
        assertEquals(40, policy.settingsButtonSizeDp)
        assertEquals(20, policy.settingsIconSizeDp)
    }

    @Test
    fun `info comment tab bar disables tap press refraction`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val tabBarBlock = source
            .substringAfter("fun VideoContentTabBar(")
            .substringBefore("// [新增] 恢复画面按钮")

        assertTrue(tabBarBlock.contains("tapPressRefractionEnabled = false"))
    }

    @Test
    fun `ios preset uses calmer intro comment tab switch motion`() {
        val iosSpec = resolveVideoContentTabSwitchAnimationSpec(UiPreset.IOS)
        val md3Spec = resolveVideoContentTabSwitchAnimationSpec(UiPreset.MD3)

        assertEquals(360, iosSpec.durationMs)
        assertEquals(240, md3Spec.durationMs)
        assertTrue(iosSpec.durationMs > md3Spec.durationMs)
        assertEquals(iosSpec.durationMs, resolveInlinePortraitPlayerCommentCollapseDurationMillis(iosSpec))
        assertEquals(md3Spec.durationMs, resolveInlinePortraitPlayerCommentCollapseDurationMillis(md3Spec))
    }

    @Test
    fun `effective selected tab follows target while pager is switching`() {
        assertEquals(
            1,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 1,
                isScrollInProgress = true,
                pageCount = 2
            )
        )
    }

    @Test
    fun `effective selected tab uses current page when pager is idle`() {
        assertEquals(
            0,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 1,
                isScrollInProgress = false,
                pageCount = 2
            )
        )
    }

    @Test
    fun `effective selected tab falls back to current page for invalid target`() {
        assertEquals(
            0,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 3,
                isScrollInProgress = true,
                pageCount = 2
            )
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
