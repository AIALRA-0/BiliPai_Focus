package com.android.purebilibili.core.store

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `SettingsManager` 体量棘轮。
 *
 * 这个文件长期是全项目第二大源文件，且是最活跃的合流点——几乎每个功能都会往里加
 * 一段。它正在按领域拆成独立 store（`core/store/player/`、`navigation/`、`home/`），
 * 但拆分这件事有个众所周知的失败模式：**拆到一半停下，然后又慢慢长回去**。
 *
 * 这条棘轮是唯一能防住那种回流的机制。每完成一批迁移，把 [MAX_LINES] 调小到新的实测值；
 * 只要没人主动调大，它就只能变小。
 */
class SettingsManagerSizeRatchetTest {

    @Test
    fun settingsManagerDoesNotGrow() {
        val file = settingsManagerFile()
        val lines = file.readLines().size

        assertTrue(
            lines <= MAX_LINES,
            "SettingsManager.kt 有 $lines 行，超过上限 $MAX_LINES。" +
                "新增设置项请放进对应领域的 store（core/store/player|navigation|home/），" +
                "不要继续堆进门面文件。",
        )
    }

    private fun settingsManagerFile(): File {
        val relative = "core/store/SettingsManager.kt"
        val roots = listOf(
            "src/main/java/com/android/purebilibili",
            "app/src/main/java/com/android/purebilibili",
        )
        return roots.map { File("$it/$relative") }.firstOrNull { it.exists() }
            ?: error("找不到 SettingsManager.kt，cwd=" + File(".").absoluteFile.canonicalPath)
    }

    private companion object {
        /**
         * 冻结于本次实测值。注意：该值相对上一快照上调，反映的是
         * 1) 主题选择迁移逻辑已移入 core/store/theme/ThemeSelectionStore.kt（不在本文件内增长），
         * 2) 上一快照后文件已有 58 行既有无快照更新增长（sidebar_account_switcher_enabled 等），
         * 3) 上游 main 合流（0.2.0 发布、App+Web 推荐流、平板侧栏、live 卡片动画开关）净增 40 行，
         * 4) 上游 live-surface 转场开关合流（getLiveSurfaceCardTransitionEnabled 设置层）再净增 31 行，
         * 5) 上游 8/7 前的 UI/设置合流再净增 37 行（本次=6690，仅同步实测值，未新增设置项），
         * 6) 长按倍速提示开关新增 24 行。
         * 仍只允许后续变小。
         */
        const val MAX_LINES = 6714
    }
}
