package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.android.purebilibili.testutil.readProjectSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 角色完备性守卫:三条生成路径(iOS 静态、静态 MD3、materialkolor 动态)
 * 都必须显式产出官方 ColorScheme 全部角色,不能依赖 Compose 默认紫调 baseline。
 */
class ThemeColorSchemeCompletenessTest {

    private val seed = Color(0xFF007AFF)
    private val lightBaseline = lightColorScheme()

    private fun ColorScheme.roles(): List<Pair<String, Color>> = listOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inversePrimary" to inversePrimary,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    private fun assertAllRolesExplicit(
        scheme: ColorScheme,
        label: String,
        expectedError: List<Color>,
    ) {
        val roles = scheme.roles().toMap()
        val missing = roles.filterValues { it == Color.Unspecified }.keys
        assertTrue(missing.isEmpty(), "$label has unspecified color roles: $missing")
        listOf("error", "onError", "errorContainer", "onErrorContainer")
            .zip(expectedError)
            .forEach { (role, expected) -> assertEquals(expected, roles[role], "$label $role") }
    }

    private fun assertStaticMd3SchemesDeclareEveryRole() {
        val source = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/theme/Theme.kt"
        )
        val functionStart = source.indexOf("internal fun createStaticMd3ColorScheme(")
        assertTrue(functionStart >= 0)
        for (factory in listOf("darkColorScheme", "lightColorScheme")) {
            val callStart = source.indexOf("$factory(", functionStart)
            assertTrue(callStart >= 0, "$factory call is missing")
            val open = source.indexOf('(', callStart)
            var depth = 0
            var close = -1
            for (index in open until source.length) {
                when (source[index]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) {
                            close = index
                            break
                        }
                    }
                }
            }
            assertTrue(close > open, "$factory arguments are incomplete")
            val assignments = Regex("(?m)^\\s*([A-Za-z][A-Za-z0-9]*)\\s*=")
                .findAll(source.substring(open + 1, close))
                .map { it.groupValues[1] }
                .toSet()
            val missing = lightBaseline.roles().map { it.first }.filterNot(assignments::contains)
            assertTrue(missing.isEmpty(), "$factory omits ColorScheme roles: $missing")
        }
    }

    private fun assertSurfaceContainerOrdered(scheme: ColorScheme, label: String) {
        val lightOrder = listOf(
            scheme.surfaceContainerLowest.luminance(),
            scheme.surfaceContainerLow.luminance(),
            scheme.surfaceContainer.luminance(),
            scheme.surfaceContainerHigh.luminance(),
            scheme.surfaceContainerHighest.luminance(),
        )
        val monotonic = lightOrder.zipWithNext().all { (a, b) -> a > b } ||
            lightOrder.zipWithNext().all { (a, b) -> a < b }
        assertTrue(monotonic, "$label surfaceContainer 五级必须单调递进,实际=$lightOrder")
    }

    // --- iOS 静态方案 ---
    // --- 静态 MD3 方案 ---

    @Test
    fun `static md3 light scheme explicitly sets all roles`() {
        assertStaticMd3SchemesDeclareEveryRole()
        val scheme = createStaticMd3ColorScheme(seed, darkTheme = false, amoledDarkTheme = false)
        assertAllRolesExplicit(
            scheme, "静态 MD3 light",
            expectedError = listOf(
                Color(0xFFB3261E), Color(0xFFFFFFFF), Color(0xFFF9DEDC), Color(0xFF410E0B)
            )
        )
        assertSurfaceContainerOrdered(scheme, "静态 MD3 light")
        assertEquals(Color.Black, scheme.scrim)
        assertEquals(scheme.primary, scheme.surfaceTint)
    }

    @Test
    fun `static md3 dark scheme explicitly sets all roles`() {
        assertStaticMd3SchemesDeclareEveryRole()
        val scheme = createStaticMd3ColorScheme(seed, darkTheme = true, amoledDarkTheme = false)
        assertAllRolesExplicit(
            scheme, "静态 MD3 dark",
            expectedError = listOf(
                Color(0xFFF2B8B5), Color(0xFF601410), Color(0xFF8C1D18), Color(0xFFF9DEDC)
            )
        )
        assertSurfaceContainerOrdered(scheme, "静态 MD3 dark")
        assertEquals(Color.Black, scheme.scrim)
    }

    @Test
    fun `static md3 amoled scheme explicitly sets all roles`() {
        assertStaticMd3SchemesDeclareEveryRole()
        val scheme = createStaticMd3ColorScheme(seed, darkTheme = true, amoledDarkTheme = true)
        assertAllRolesExplicit(
            scheme, "静态 MD3 amoled",
            expectedError = listOf(
                Color(0xFFF2B8B5), Color(0xFF601410), Color(0xFF8C1D18), Color(0xFFF9DEDC)
            )
        )
        // amoled 覆盖会把 surfaceContainer 压到近纯黑,五级单调性由既有覆盖逻辑保证,不再断言
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(Color.Black, scheme.scrim)
    }

    // --- materialkolor 动态路径 ---

    @Test
    fun `materialkolor dynamic scheme generates all roles for light and dark`() {
        val light = alignStaticColorSchemeWithThemePrimary(
            scheme = dynamicColorScheme(
                seedColor = seed,
                isDark = false,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            ),
            themePrimaryColor = seed,
            darkTheme = false,
        )
        val dark = alignStaticColorSchemeWithThemePrimary(
            scheme = dynamicColorScheme(
                seedColor = seed,
                isDark = true,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            ),
            themePrimaryColor = seed,
            darkTheme = true,
        )

        assertAllRolesExplicit(light, "materialkolor light", expectedError = listOf(light.error, light.onError, light.errorContainer, light.onErrorContainer))
        assertAllRolesExplicit(dark, "materialkolor dark", expectedError = listOf(dark.error, dark.onError, dark.errorContainer, dark.onErrorContainer))
        assertSurfaceContainerOrdered(light, "materialkolor light")
        assertSurfaceContainerOrdered(dark, "materialkolor dark")
        assertEquals(Color.Black, light.scrim)
        assertEquals(Color.Black, dark.scrim)
        // align 后 surfaceTint 与种子色 primary 一致
        assertEquals(seed, light.surfaceTint)
        assertEquals(seed, dark.surfaceTint)
    }
}
