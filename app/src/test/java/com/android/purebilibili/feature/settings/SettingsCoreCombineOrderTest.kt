package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 守卫 `coreSettingsFlow` 的下标约定。
 *
 * 这个 combine 按下标取值（`values[i] as X`），所以新增偏好**只能追加到末尾**：
 * 插在中间会让后面所有项整体错位，运行时表现为类型转换崩溃
 * （曾出现 `java.lang.Boolean cannot be cast to DarkThemeSchedule`）。
 */
class SettingsCoreCombineOrderTest {

    @Test
    fun `core settings flows keep the frozen index order`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt"),
            File("../app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt"),
        ).first(File::exists).readText()

        val start = source.indexOf("private val coreSettingsFlow = combine(")
        val end = source.indexOf(") { values ->", start)
        assertTrue(start >= 0 && end > start, "coreSettingsFlow section markers missing")

        val flows = Regex("SettingsManager\\.(\\w+)\\(context\\)")
            .findAll(source.substring(start, end))
            .map { it.groupValues[1] }
            .toList()

        assertEquals(
            listOf(
                "getUiStyle",
                "getHwDecode",
                "getThemeMode",
                "getDarkThemeStyle",
                "getAppLanguage",
                "getDynamicColor",
                "getMd3ColorSource",
                "getMd3CustomColorHex",
                "getThemeColorStyle",
                "getThemeColorSpec",
                "getMiniPlayerMode",
                "getDarkThemeSchedule",
                "getThemeColorContrast",
            ),
            flows,
            "coreSettingsFlow 按下标取值：新增偏好只能追加到末尾，插在中间会让后面所有项错位",
        )
        assertEquals(
            "getThemeColorContrast",
            flows.last(),
            "新增偏好必须排在最后一项",
        )
    }
}
