package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.PresetPrimitiveRenderer
import com.android.purebilibili.core.ui.resolveAppNavigationCapabilities
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SideBarRendererPolicyTest {

    @Test
    fun miuixVariantRoutesToOfficialNavigationRail() {
        assertEquals(
            true,
            resolveAppNavigationCapabilities(PresetPrimitiveRenderer.MIUIX_BRIDGED).usePlatformSideRail
        )
    }

    @Test
    fun materialKeepsFrostedSideBar() {
        assertEquals(
            false,
            resolveAppNavigationCapabilities(PresetPrimitiveRenderer.MATERIAL3).usePlatformSideRail
        )
    }

    @Test
    fun expandableRailOnlyOnExpandedWidthClass() {
        assertTrue(shouldUseExpandableMiuixSideBar(isExpandedWidthClass = true))
        assertFalse(shouldUseExpandableMiuixSideBar(isExpandedWidthClass = false))
    }

    @Test
    fun officialSideBarItemRequiresNoSkinBitmap() {
        assertTrue(shouldUseMiuixOfficialSideBarItem(skinIconPath = null))
        assertFalse(shouldUseMiuixOfficialSideBarItem(skinIconPath = "/skin/home.png"))
    }

    @Test
    fun homeDoubleTapMapsFromRapidSuccessiveClicks() {
        assertEquals(
            HomeSideBarClickAction.NAVIGATE,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.HOME,
                nowMs = 1_000L,
                lastHomeClickMs = 0L
            )
        )
        assertEquals(
            HomeSideBarClickAction.HOME_DOUBLE_TAP,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.HOME,
                nowMs = 1_200L,
                lastHomeClickMs = 1_000L
            )
        )
        assertEquals(
            HomeSideBarClickAction.NAVIGATE,
            resolveHomeSideBarClickAction(
                item = BottomNavItem.DYNAMIC,
                nowMs = 1_200L,
                lastHomeClickMs = 1_000L
            )
        )
    }

    @Test
    fun dynamicDoubleTapMapsFromRapidSuccessiveClicks() {
        assertFalse(
            resolveDynamicSideBarClickAction(
                item = BottomNavItem.DYNAMIC,
                nowMs = 1_000L,
                lastDynamicClickMs = 0L
            )
        )
        assertTrue(
            resolveDynamicSideBarClickAction(
                item = BottomNavItem.DYNAMIC,
                nowMs = 1_200L,
                lastDynamicClickMs = 1_000L
            )
        )
        assertFalse(
            resolveDynamicSideBarClickAction(
                item = BottomNavItem.HOME,
                nowMs = 1_200L,
                lastDynamicClickMs = 1_000L
            )
        )
        assertFalse(
            resolveDynamicSideBarClickAction(
                item = BottomNavItem.DYNAMIC,
                nowMs = 1_400L,
                lastDynamicClickMs = 1_000L
            )
        )
    }

    @Test
    fun frostedSideBarSourceRoutesMiuixBranchToOfficialRail() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt")
            .takeIf { it.exists() }
            ?.readText()
            ?: File("app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt").readText()

        assertTrue(source.contains("rememberAppNavigationCapabilities()"))
        assertTrue(source.contains("AppPlatformNavigationRail("))
        assertTrue(source.contains("AppPlatformNavigationRailItem("))
        assertTrue(source.contains("expanded = expandable"))
        assertTrue(source.contains("initiallyExpanded = sidebarExpanded"))
        assertTrue(source.contains("onExpandedChange = onSidebarExpandedChange"))
        assertTrue(source.contains("shouldUseExpandableMiuixSideBar("))
        assertTrue(source.contains("shouldUseMiuixOfficialSideBarItem(skinIconPath)"))
        assertTrue(source.contains("MiuixSideBarSkinItem("))
        assertTrue(source.contains("label = \"\${item.name}_miuix_side_bar\""))
        assertTrue(source.contains("modifier = animatedItemModifier"))
        assertTrue(source.contains("SideBarAccountSwitchButton("))
        assertTrue(source.contains("onAccountSwitchClick"))
        assertTrue(source.contains("resolveHomeSideBarClickAction("))
        assertFalse(source.contains("import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail"))
        assertFalse(source.contains("import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem"))
    }

    @Test
    fun materialSideBarUsesMd3IconPairsWithSharedSelectionTransform() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt")
            .takeIf { it.exists() }
            ?.readText()
            ?: File("app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt").readText()
        val materialSideBarSource = source.substringAfter("private fun FrostedSideBarContent(")

        assertTrue(source.contains("FrostedSideBarBlendedMaterialIcon("))
        assertTrue(source.contains("resolveMaterialBottomBarIcon(item, selected = false)"))
        assertTrue(source.contains("resolveMaterialBottomBarIcon(item, selected = true)"))
        assertTrue(materialSideBarSource.contains("FrostedSideBarBlendedMaterialIcon("))
        assertFalse(materialSideBarSource.contains("resolveHomeNavigationBarIcon("))
        assertTrue(materialSideBarSource.contains("rememberNavigationSelectionTransform("))
        assertTrue(materialSideBarSource.contains("rotationZ = selectionTransform.rotationDegrees()"))
        assertTrue(materialSideBarSource.contains("scaleX = selectionTransform.scale()"))
    }
}
