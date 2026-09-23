package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenSourceLicensesPolicyTest {

    @Test
    fun openSourceLibraries_coverRuntimeDependenciesAndLiquidGlassReferences() {
        val names = openSourceLibraries.map { it.name }.toSet()

        listOf(
            "Kotlin",
            "Jetpack Compose",
            "Miuix",
            "AndroidX Media3 / ExoPlayer",
            "OkHttp",
            "Retrofit",
            "Coil",
            "Haze",
            // "Compose Shimmer" 已移除：依赖本身已从 app/build.gradle.kts 删掉
            // （全仓 0 处 import，骨架屏用的是自研实现），不再随包分发，
            // 因此也不应继续出现在致谢列表里。
            "Cupertino",
            "Backdrop",
            "BBPlayer",
            "DanmakuRenderEngine",
            // "Cling" 已移除：193325e7「refactor: simplify dlna casting stack」删掉了
            // 依赖与致谢条目却没同步这份清单，这条测试自那时起一直是红的。
            // 现在全仓已无任何 Cling 引用，只在打包排除规则的注释里提过一次。
            "NanoHTTPD",
            "Turbine",
            "KernelSU"
        ).forEach { name ->
            assertTrue(name in names, "$name 应出现在开源致谢列表中")
        }
    }

    @Test
    fun openSourceLibraries_showGitHubLinksAndLicenses() {
        assertTrue(openSourceLibraries.isNotEmpty())
        openSourceLibraries.forEach { library ->
            assertTrue(library.license.isNotBlank(), "${library.name} 需要注明开源许可")
            assertTrue(library.url.startsWith("https://github.com/"), "${library.name} 需要提供 GitHub 链接")
        }
        assertEquals(
            "https://github.com/tiann/KernelSU",
            openSourceLibraries.first { it.name == "KernelSU" }.url
        )
    }

    @Test
    fun openSourceScreen_notesAcknowledgementListMayNotBeExhaustive() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/settings/screen/OpenSourceLicensesScreen.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/settings/screen/OpenSourceLicensesScreen.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("可能不包含全部传递依赖或完整法律清单"))
        assertTrue(source.contains("MaterialTheme.colorScheme.primary"))
    }
}
