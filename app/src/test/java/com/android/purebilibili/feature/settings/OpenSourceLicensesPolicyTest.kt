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
            "Compose Shimmer",
            "Cupertino",
            "Backdrop",
            "DanmakuRenderEngine",
            "Cling",
            "NanoHTTPD",
            "Turbine",
            "KernelSU",
            "NagramX"
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
        assertEquals(
            "https://github.com/risin42/NagramX",
            openSourceLibraries.first { it.name == "NagramX" }.url
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
