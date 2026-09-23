package com.android.purebilibili.core.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSemanticVisualPolicyTest {

    private val palette = AppSemanticAccentPalette(
        primary = Color(0xFF112233),
        secondary = Color(0xFF223344),
        tertiary = Color(0xFF334455),
        error = Color(0xFF445566),
    )

    @Test
    fun twoValueStylesUseTheirNativeSemanticIconFamily() {
        listOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX).forEach { style ->
            val policy = resolveAppSemanticVisualPolicy(
                uiStyle = style,
                materialPalette = palette,
            )

            assertEquals(
                if (style == AppUiStyle.MIUIX) AppSemanticIconFamily.MIUIX else AppSemanticIconFamily.MATERIAL,
                policy.iconFamily,
            )
            assertEquals(
                style == AppUiStyle.MIUIX,
                policy.prefersGroupedListCards,
            )
            assertEquals(palette, policy.accentPalette)
        }
    }

    @Test
    fun chromeLiquidGlassOnlyHonorsGlobalEntry() {
        assertFalse(
            resolveAppChromeLiquidGlassEnabled(
                androidNativeEnabled = false,
            )
        )
        assertEquals(
            true,
            resolveAppChromeLiquidGlassEnabled(
                androidNativeEnabled = true,
            )
        )
    }

    @Test
    fun staticMaterialPaletteCollapsesNonErrorAccentsToPrimary() {
        val scheme = lightColorScheme(
            primary = Color(0xFFAA3366),
            secondary = Color(0xFF335577),
            tertiary = Color(0xFF556677),
            error = Color(0xFFCC1122),
        )
        val staticPalette = resolveAppSemanticAccentPalette(
            colorScheme = scheme,
            useSemanticAccentRoles = false,
        )

        assertEquals(scheme.primary, staticPalette.primary)
        assertEquals(scheme.primary, staticPalette.secondary)
        assertEquals(scheme.primary, staticPalette.tertiary)
        assertEquals(scheme.error, staticPalette.error)
    }

    @Test
    fun settingsSemanticPoliciesDoNotReadConcreteStyles() {
        val paths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsEntryVisualPolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsSemanticIconPolicy.kt",
        )
        val styleDependency = Regex(
            """\b(UiPreset|UiStyle|AppUiStyle|AndroidNativeVariant|LocalUiPreset|LocalUiStyle|LocalAppUiStyle|LocalAndroidNativeVariant)\b"""
        )

        paths.forEach { path ->
            val source = loadSource(path)
            assertFalse(styleDependency.containsMatchIn(source), "Concrete style dependency remains in $path")
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath), File("../$path"))
            .firstOrNull(File::exists)
            ?.readText()
            ?.replace("\r\n", "\n")
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
