package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class IOSGroupSurfaceShapeStructureTest {

    @Test
    fun `ios group passes rounded shape into surface for md3 and miuix borders`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt")
        val iosGroupSource = source
            .substringAfter("fun IOSGroup(")
            .substringBefore("@Composable\nfun IOSSwitchItem")

        assertTrue(iosGroupSource.contains(".clip(appliedShape)"))
        assertTrue(iosGroupSource.contains("Surface("))
        assertTrue(iosGroupSource.contains("shape = appliedShape,"))
    }

    @Test
    fun `miuix grouped settings use native card and preference rows`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt")
        val iosGroupSource = source
            .substringAfter("fun IOSGroup(")
            .substringBefore("@Composable\nfun IOSSwitchItem")
        val switchItemSource = source
            .substringAfter("fun IOSSwitchItem(")
            .substringBefore("@Composable\nfun IOSClickableItem")
        val clickableItemSource = source
            .substringAfter("fun IOSClickableItem(")
            .substringBefore("@Composable\nfun IOSSearchBar")

        assertTrue(source.contains("Card as MiuixCard"))
        assertTrue(source.contains("SwitchPreference as MiuixSwitchPreference"))
        assertTrue(source.contains("ArrowPreference as MiuixArrowPreference"))
        assertTrue(iosGroupSource.contains("MiuixCard("))
        assertTrue(switchItemSource.contains("MiuixSwitchPreference("))
        assertTrue(clickableItemSource.contains("MiuixArrowPreference("))
    }

    @Test
    fun `switch item uses measured row layout so trailing switch cannot overlap text`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt")
        val switchItemSource = source
            .substringAfter("fun IOSSwitchItem(")
            .substringBefore("@Composable\nfun IOSClickableItem")

        assertTrue(switchItemSource.contains("Row("))
        assertTrue(switchItemSource.contains("Column(modifier = Modifier.weight(1f))"))
        assertTrue(switchItemSource.contains("Spacer(modifier = Modifier.width(rowSpec.trailingSpacingDp.dp))"))
        assertTrue(!switchItemSource.contains("BasicComponent("))
    }

    @Test
    fun `miuix switch item respects app haptic setting`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/components/iOSListComponents.kt")
        val switchItemSource = source
            .substringAfter("fun IOSSwitchItem(")
            .substringBefore("@Composable\nfun IOSClickableItem")

        assertTrue(
            switchItemSource.contains("SettingsManager.isHapticFeedbackEnabledSync"),
            "Miuix switch 内部触感必须受应用触感反馈开关控制"
        )
        assertTrue(
            switchItemSource.contains("NoOpHapticFeedback"),
            "触感关闭时必须用 no-op LocalHapticFeedback 屏蔽 Miuix 内部震动"
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
