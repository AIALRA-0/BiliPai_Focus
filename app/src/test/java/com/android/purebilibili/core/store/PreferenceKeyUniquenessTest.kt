package com.android.purebilibili.core.store

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DataStore key 字面量的重复度棘轮。
 *
 * `SettingsManager` 正在按领域拆成多个 store。这个过程里最危险的 bug 不是编译错误，
 * 而是**同一个 key 字符串在两个文件里各定义一次**：两处各自读写、各自缓存，
 * 语义悄悄分叉，而且因为指向同一个存储槽，测试和肉眼都很难发现。
 *
 * 拆分时 key 的定义应当**整体搬走**，而不是复制一份留在原处。
 *
 * 这里不要求「全局唯一」——那会直接卡住正在进行的拆分——而是冻结当前的重复数量，
 * 只允许减少。每完成一个 store 的迁移，把 [MAX_DUPLICATED_KEYS] 调小。
 */
class PreferenceKeyUniquenessTest {

    @Test
    fun duplicatedPreferenceKeysDoNotGrow() {
        val duplicates = collectDuplicatedKeys()

        assertTrue(
            duplicates.size <= MAX_DUPLICATED_KEYS,
            buildString {
                appendLine(
                    "跨文件重复的 DataStore key 有 ${duplicates.size} 个，" +
                        "超过上限 $MAX_DUPLICATED_KEYS。",
                )
                appendLine("拆分 store 时请把 key 定义搬走而不是复制：")
                duplicates.toSortedMap().forEach { (key, files) ->
                    appendLine("  \"$key\" -> ${files.sorted().joinToString(", ")}")
                }
            },
        )
    }

    @Test
    fun duplicatedPreferenceKeyOwnersMatchMigrationSnapshot() {
        assertEquals(
            EXPECTED_DUPLICATED_KEY_OWNERS,
            collectDuplicatedKeys(),
            "重复 key 的数量没变并不代表所有权没扩散。拆分 store 时必须保留精确的 key -> 文件映射；" +
                "完成迁移后再有意更新快照。",
        )
    }

    /**
     * key 的字符串字面量本身绝不能改——改了等于让所有老用户的该项设置静默回到默认值。
     * 这条守住迁移过程中最不可逆的一类错误。
     */
    @Test
    fun knownCriticalKeysKeepTheirLiterals() {
        val allKeys = collectKeyOccurrences().keys
        CRITICAL_KEYS.forEach { key ->
            assertTrue(
                key in allKeys,
                "关键 key \"$key\" 不见了。若是拆分时改名，所有老用户的该项设置会静默" +
                    "回到默认值；key 字面量必须原样保留。",
            )
        }
    }

    private fun collectDuplicatedKeys(): Map<String, Set<String>> =
        collectKeyOccurrences().filterValues { it.size > 1 }

    private fun collectKeyOccurrences(): Map<String, Set<String>> {
        val occurrences = mutableMapOf<String, MutableSet<String>>()
        storeSourceFiles().forEach { file ->
            KEY_PATTERN.findAll(file.readText()).forEach { match ->
                val literal = match.groupValues[1]
                // 跳过带插值的模板 key（如 plugin_enabled_$pluginId）：它们是参数化的，
                // 同一文件里出现多次是正常写法。
                if (literal.contains('$')) return@forEach
                occurrences.getOrPut(literal) { mutableSetOf() }.add(file.name)
            }
        }
        return occurrences
    }

    private fun storeSourceFiles(): Sequence<File> {
        val root = candidateRoots.firstOrNull { File(it).exists() }
            ?: error(
                "找不到 main 源码根目录，cwd=" + File(".").absoluteFile.canonicalPath,
            )
        return File(root).walkTopDown().filter { it.isFile && it.extension == "kt" }
    }

    private companion object {
        val candidateRoots = listOf(
            "src/main/java/com/android/purebilibili",
            "app/src/main/java/com/android/purebilibili",
        )

        val KEY_PATTERN =
            Regex("""(?:boolean|int|long|float|double|string|stringSet)PreferencesKey\(\s*"([^"]+)"""")

        /**
         * 冻结于 SettingsManager 拆分进行中的实测值：SettingsManager 与
         * PlayerSettingsStore / NavigationSettingsStore 之间的重叠。只能调小。
         * sidebar_account_switcher_enabled 为上一快照后既有新增项，本次一并纳入快照。
         */
        const val MAX_DUPLICATED_KEYS = 10

        val EXPECTED_DUPLICATED_KEY_OWNERS = mapOf(
            "bottom_bar_order" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "bottom_bar_visible_tabs" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "default_playback_speed" to setOf("PlayerSettingsStore.kt", "SettingsManager.kt"),
            "last_playback_speed" to setOf("PlayerSettingsStore.kt", "SettingsManager.kt"),
            "predictive_back_animation_style" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "predictive_back_enabled" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "predictive_back_exit_direction" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "remember_last_playback_speed" to setOf("PlayerSettingsStore.kt", "SettingsManager.kt"),
            "sidebar_account_switcher_enabled" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
            "tablet_use_sidebar" to setOf("NavigationSettingsStore.kt", "SettingsManager.kt"),
        )

        val CRITICAL_KEYS = listOf(
            "theme_mode_v2",
            "ui_preset",
            "card_transition_enabled",
            "default_playback_speed",
            "bottom_bar_order",
            "runtime_visual_guard_enabled",
        )
    }
}
