package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveGroupSurfaceShapeStructureTest {

    @Test
    fun `group renderer branches on dual value ui style`() {
        val source = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt")
        val iosGroupSource = source
            .substringAfter("fun AdaptivePreferenceGroupRenderer(")
            .substringBefore("@Composable\ninternal fun AdaptiveSwitchPreferenceContent")

        assertFalse(iosGroupSource.contains(".clip(appliedShape)"))
        assertFalse(iosGroupSource.contains("resolveAdaptiveGroupSurfaceShape("))
        assertFalse(iosGroupSource.contains("UiPreset"))
        assertTrue(iosGroupSource.contains("LocalAppUiStyle.current"))
        assertTrue(iosGroupSource.contains("if (uiStyle == AppUiStyle.MIUIX) {"))
        assertTrue(iosGroupSource.contains("MiuixCard("))
        assertTrue(iosGroupSource.contains("Surface("))
        assertTrue(iosGroupSource.contains("shape = appliedShape,"))
    }

    @Test
    fun `miuix grouped settings use native card and measured preference rows`() {
        val source = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt")
        val iosGroupSource = source
            .substringAfter("fun AdaptivePreferenceGroupRenderer(")
            .substringBefore("@Composable\ninternal fun AdaptiveSwitchPreferenceContent")
        val switchItemSource = source
            .substringAfter("fun AdaptiveSwitchPreferenceContent(")
            .substringBefore("@Composable\nfun AdaptiveSliderPreferenceRenderer")
        val clickableItemSource = source
            .substringAfter("fun AdaptivePreferenceContent(")
            .substringBefore("@Composable\nfun AdaptiveSearchFieldRenderer")

        assertTrue(source.contains("Card as MiuixCard"))
        assertTrue(source.contains("SwitchPreference as MiuixSwitchPreference"))
        assertTrue(iosGroupSource.contains("MiuixCard("))
        assertTrue(switchItemSource.contains("MiuixSwitchPreference("))
        assertTrue(clickableItemSource.contains("BasicComponent("))
        assertTrue(clickableItemSource.contains("Icons.AutoMirrored.Filled.KeyboardArrowRight"))
        assertFalse(clickableItemSource.contains("MiuixArrowPreference("))
    }

    @Test
    fun `switch item uses measured row layout so trailing switch cannot overlap text`() {
        val source = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt")
        val switchItemSource = source
            .substringAfter("fun AdaptiveSwitchPreferenceContent(")
            .substringBefore("@Composable\nfun AdaptiveSliderPreferenceRenderer")

        assertTrue(switchItemSource.contains("Row("))
        assertTrue(switchItemSource.contains("Column(modifier = Modifier.weight(1f))"))
        assertTrue(switchItemSource.contains("Spacer(modifier = Modifier.width(rowSpec.trailingSpacingDp.dp))"))
        assertTrue(!switchItemSource.contains("BasicComponent("))
    }

    @Test
    fun `md3 clickable item uses measured wrapping text instead of basic component`() {
        val source = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt")
        val clickableItemSource = source
            .substringAfter("fun AdaptivePreferenceContent(")
            .substringBefore("@Composable\nfun AdaptiveSearchFieldRenderer")
        val md3Block = clickableItemSource
            .substringAfter("if (clickableRenderer == AppClickableItemRenderer.MD3_BASIC)")
            .substringBefore("if (clickableRenderer != AppClickableItemRenderer.MD3_BASIC)")

        assertTrue(md3Block.contains("Column(modifier = Modifier.weight(1f))"))
        assertTrue(md3Block.contains("text = title"))
        assertTrue(md3Block.contains("text = subtitle"))
        // Title stays single-line; trailing value may wrap within shared max width.
        assertTrue(md3Block.contains("maxLines = 1"))
        assertTrue(md3Block.contains("appPreferenceValueTextModifier()"))
        assertTrue(md3Block.contains("APP_PREFERENCE_VALUE_MAX_LINES"))
        assertTrue(md3Block.contains("softWrap = true"))
        assertFalse(md3Block.contains("widthIn(max = 120.dp)"))
        assertFalse(md3Block.contains("BasicComponent("))
    }

    @Test
    fun preferenceValueLayout_isSharedAcrossRenderers() {
        val source = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt",
        )
        assertTrue(source.contains("APP_PREFERENCE_VALUE_MAX_WIDTH_DP = 200"))
        assertTrue(source.contains("APP_PREFERENCE_VALUE_MAX_LINES = 2"))
        assertTrue(source.contains("fun Modifier.appPreferenceValueTextModifier()"))
        // All trailing-value paths must share the helper (no leftover 120.dp cap).
        assertFalse(
            Regex("""widthIn\(max = 120\.dp\)""").containsMatchIn(source),
            "Preference value width must not hard-cap at 120.dp",
        )
        val valueHelperUses = Regex("""\.appPreferenceValueTextModifier\(\)""").findAll(source).count()
        assertTrue(
            valueHelperUses >= 4,
            "Expected shared value modifier on MD3/Miuix/fallback paths, found $valueHelperUses",
        )
    }

    @Test
    fun `miuix switch item respects app haptic setting`() {
        val source = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt")
        val switchItemSource = source
            .substringAfter("fun AdaptiveSwitchPreferenceContent(")
            .substringBefore("@Composable\nfun AdaptiveSliderPreferenceRenderer")

        assertTrue(
            switchItemSource.contains("LocalAppThemeConfig.current.hapticFeedbackEnabled"),
            "Miuix switch 内部触感必须受注入的应用主题配置控制"
        )
        assertFalse(
            switchItemSource.contains("SettingsManager"),
            "core/ui 组件不得反向读取设置存储"
        )
        assertTrue(
            switchItemSource.contains("NoOpHapticFeedback"),
            "触感关闭时必须用 no-op LocalHapticFeedback 屏蔽 Miuix 内部震动"
        )
    }

    private fun loadSource(path: String): String {
        val sourceFile = listOf(
            File(path),
            File("../$path"),
            File(path.removePrefix("design-system/")),
            File(path.removePrefix("app/")),
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
