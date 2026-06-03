package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.android.purebilibili.core.theme.BottomBarColors
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarColorBindingPolicyTest {

    @Test
    fun `resolves custom color by enum name`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = mapOf("DYNAMIC" to 6)
        )

        assertEquals(6, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `resolves custom color by lowercase route key`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = mapOf("dynamic" to 4)
        )

        assertEquals(4, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `resolves custom color by legacy chinese alias`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.WATCHLATER,
            itemColorIndices = mapOf("稍后再看" to 5)
        )

        assertEquals(5, binding.colorIndex)
        assertTrue(binding.hasCustomAccent)
    }

    @Test
    fun `falls back to default when no key matches`() {
        val binding = resolveBottomBarItemColorBinding(
            item = BottomNavItem.DYNAMIC,
            itemColorIndices = emptyMap()
        )

        assertEquals(0, binding.colorIndex)
        assertFalse(binding.hasCustomAccent)
    }

    @Test
    fun `selected foreground uses theme color when no custom color is configured`() {
        val themeColor = Color(0xFF007AFF)
        val color = resolveBottomBarSelectedContentColor(
            item = BottomNavItem.DYNAMIC,
            binding = BottomBarItemColorBinding(colorIndex = 0, hasCustomAccent = false),
            themeColor = themeColor
        )

        assertEquals(themeColor, color)
    }

    @Test
    fun `bottom bar selected icons use filled symbols`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
            .readText()
        val selectedSymbols = listOf(
            "House",
            "Bell",
            "PlayCircle",
            "Clock",
            "Person",
            "Star",
            "Video",
            "Gearshape"
        )

        assertTrue(
            selectedSymbols.all { symbol ->
                source.contains("{ Icon(CupertinoIcons.Filled.$symbol, contentDescription = null) }")
            },
            "Bottom bar selected icons should use filled symbols so the whole selected icon is tinted by the theme color."
        )
    }

    @Test
    fun `watch later bottom bar icons use clock semantics`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
            .readText()
        val watchLaterBlock = source
            .substringAfter("WATCHLATER(")
            .substringBefore("    ),")

        assertTrue(watchLaterBlock.contains("CupertinoIcons.Filled.Clock"))
        assertTrue(watchLaterBlock.contains("CupertinoIcons.Outlined.Clock"))
        assertFalse(watchLaterBlock.contains("Bookmark"))
        assertTrue(
            source.contains(
                "BottomNavItem.WATCHLATER -> if (selected) Icons.Filled.WatchLater else Icons.Outlined.WatchLater"
            )
        )
    }

    @Test
    fun `sliding indicator uses filled icon for the covered item`() {
        val visual = resolveBottomBarItemMotionVisual(
            itemIndex = 1,
            indicatorPosition = 1f,
            currentSelectedIndex = 0,
            motionProgress = 0.7f,
            selectionEmphasis = 0.28f
        )

        assertTrue(
            visual.useSelectedIcon,
            "Item directly covered by the moving indicator should use the filled icon so theme color fills the symbol during drag."
        )
    }

    @Test
    fun `tap target does not jump to selected color before indicator arrives`() {
        val homeCoverage = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0f,
            currentSelectedIndex = 1,
            motionProgress = 0f
        )
        val tappedTargetCoverage = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0f,
            currentSelectedIndex = 1,
            motionProgress = 0f
        )
        val targetMidCoverage = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.42f,
            currentSelectedIndex = 1,
            motionProgress = 0f
        )

        assertEquals(
            1f,
            homeCoverage,
            "The visual state should stay on the indicator origin when navigation state has already switched."
        )
        assertEquals(
            0f,
            tappedTargetCoverage,
            "The tapped page must not become fully selected until the indicator reaches it."
        )
        assertEquals(
            0.42f,
            targetMidCoverage,
            0.001f,
            "The tapped page should gain color only according to indicator coverage."
        )
    }

    @Test
    fun `selected foreground uses custom item color when configured`() {
        val color = resolveBottomBarSelectedContentColor(
            item = BottomNavItem.HOME,
            binding = BottomBarItemColorBinding(colorIndex = 5, hasCustomAccent = true),
            themeColor = Color(0xFF8D6E63)
        )

        assertEquals(BottomBarColors.getColorByIndex(5), color)
    }

    @Test
    fun `sliding content color follows selection fraction continuously`() {
        val unselected = Color.Black
        val selected = Color(0xFFFF5F9A)

        assertEquals(
            lerp(unselected, selected, 0.35f),
            resolveBottomBarSlidingContentColor(
                unselectedColor = unselected,
                selectedColor = selected,
                selectionFraction = 0.35f,
                isPending = false
            )
        )
        assertEquals(
            lerp(unselected, selected, 0.5f),
            resolveBottomBarSlidingContentColor(
                unselectedColor = unselected,
                selectedColor = selected,
                selectionFraction = 0.5f,
                isPending = false
            )
        )
        assertEquals(
            lerp(unselected, selected, 0.75f),
            resolveBottomBarSlidingContentColor(
                unselectedColor = unselected,
                selectedColor = selected,
                selectionFraction = 0.75f,
                isPending = false
            )
        )
        assertEquals(
            selected,
            resolveBottomBarSlidingContentColor(
                unselectedColor = unselected,
                selectedColor = selected,
                selectionFraction = 1f,
                isPending = false
            )
        )
        assertEquals(
            unselected,
            resolveBottomBarSlidingContentColor(
                unselectedColor = unselected,
                selectedColor = selected,
                selectionFraction = 0f,
                isPending = false
            )
        )
    }

    @Test
    fun `android native item uses realtime override color without animation lag`() {
        val realtimeColor = Color(0xFF00A1D6)
        val animatedColor = Color(0xFF888888)

        assertEquals(
            realtimeColor,
            resolveAndroidNativeBottomBarItemContentColor(
                contentColorOverride = realtimeColor,
                animatedContentColor = animatedColor
            )
        )
        assertEquals(
            animatedColor,
            resolveAndroidNativeBottomBarItemContentColor(
                contentColorOverride = null,
                animatedContentColor = animatedColor
            )
        )
    }

    @Test
    fun `material docked bottom bar selected icon and text use theme primary`() {
        val themePrimary = Color(0xFF9C27B0)
        val onSurfaceVariant = Color(0xFF5F6368)
        val secondaryContainer = Color(0xFFEADDFF)

        val colors = resolveMaterialDockedBottomBarItemColors(
            themePrimary = themePrimary,
            onSurfaceVariant = onSurfaceVariant,
            secondaryContainer = secondaryContainer
        )

        assertEquals(themePrimary, colors.selectedIconColor)
        assertEquals(themePrimary, colors.selectedTextColor)
        assertEquals(onSurfaceVariant, colors.unselectedIconColor)
        assertEquals(onSurfaceVariant, colors.unselectedTextColor)
        assertEquals(secondaryContainer, colors.indicatorColor)
    }

    @Test
    fun `light mode bottom bar always uses dark readable foreground`() {
        assertEquals(
            Color.Black,
            resolveBottomBarReadableContentColor(
                isLightMode = true,
                liquidGlassProgress = 0.08f,
                contentLuminance = 0.95f
            )
        )
    }

    @Test
    fun `bright frosted backdrop flips bottom bar content to dark foreground`() {
        val color = resolveBottomBarReadableContentColor(
            isLightMode = false,
            liquidGlassProgress = 0.92f,
            contentLuminance = 0.84f
        )

        assertTrue(color.red < 0.2f)
        assertTrue(color.alpha >= 0.8f)
    }

    @Test
    fun `clear dark glass keeps bright foreground for contrast`() {
        val color = resolveBottomBarReadableContentColor(
            isLightMode = false,
            liquidGlassProgress = 0.18f,
            contentLuminance = 0.22f
        )

        assertTrue(color.red > 0.9f)
        assertTrue(color.alpha >= 0.95f)
    }
}
