package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 守护 BiliPaiNavKey 每个具体类型都必须在 BiliPaiNavEntryProvider 注册 entry。
 *
 * Miuix NavDisplay.build() 按 key 的精确运行时类查找 factory，缺注册会在重组时抛
 * "No entry { } registered for ..."。HistorySearch / WatchLaterSearch / FavoriteSearch
 * 曾漏注册导致该崩溃。
 */
class BiliPaiNavEntryRegistrationStructureTest {

    @Test
    fun everyConcreteNavKeyHasARegisteredEntry() {
        val keySource = sourceFile("BiliPaiNavKey.kt").readText()
        val providerSource = sourceFile("BiliPaiNavEntryProvider.kt").readText()

        val declaredKeys = Regex("""(?:data class|data object) ([A-Za-z]+)""")
            .findAll(keySource)
            .map { it.groupValues[1] }
            .toSet()

        val registeredKeys = Regex("""entry<BiliPaiNavKey\.([A-Za-z]+)>""")
            .findAll(providerSource)
            .map { it.groupValues[1] }
            .toSet()

        assertEquals(declaredKeys, registeredKeys)
    }

    private fun sourceFile(name: String): File {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/$name"),
            File("src/main/java/com/android/purebilibili/navigation3/$name"),
        ).first { it.exists() }
    }
}
