package com.android.purebilibili.feature.home

import com.android.purebilibili.core.ui.lint.StyleLintAllowlist
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 冻结 home 第一批低风险文件的 token 迁移状态。
 *
 * Task 5 的全量目标是整个 feature/home，本测试先锁住已经完成迁移的文件，
 * 避免后续继续推进 BottomBar / iOSHomeHeader / 卡片大文件时把小文件回退。
 */
class HomeTokenAdoptionTest {

    private val migratedHomeFiles = listOf(
        "src/main/java/com/android/purebilibili/feature/home/HomeComponents.kt",
        "src/main/java/com/android/purebilibili/feature/home/HomeGlassVisualPolicy.kt",
        "src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/CrashTrackingConsentDialog.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabChrome.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/VideoPreviewDialog.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/cards/StoryVideoCard.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
    )

    private val migratedHomeMotionFiles = listOf(
        "src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt",
        "src/main/java/com/android/purebilibili/feature/home/components/LiquidIndicator.kt"
    )

    @Test
    fun migratedHomeFiles_areNotInShapeAllowlist() {
        val leaked = migratedHomeFiles.filter { it in StyleLintAllowlist.SHAPE_HITS }
        assertTrue(
            leaked.isEmpty(),
            "已迁移的 home 文件不能继续留在 SHAPE_HITS：\n" + leaked.joinToString("\n")
        )
    }

    @Test
    fun migratedHomeFiles_areNotInSurfaceAllowlist() {
        val leaked = migratedHomeFiles.filter { it in StyleLintAllowlist.SURFACE_HITS }
        assertTrue(
            leaked.isEmpty(),
            "已迁移的 home 文件不能继续留在 SURFACE_HITS：\n" + leaked.joinToString("\n")
        )
    }

    @Test
    fun migratedHomeFiles_haveNoResidualHardcodedShapeOrSurface() {
        val shapePattern = Regex("""RoundedCornerShape\(\s*\d+""")
        val surfacePattern = Regex("""MaterialTheme\.colorScheme\.(surface|background)\b""")
        val residual = mutableListOf<String>()

        migratedHomeFiles.forEach { path ->
            val file = locate(path) ?: error("Cannot locate $path from cwd")
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (shapePattern.containsMatchIn(line) || surfacePattern.containsMatchIn(line)) {
                        residual.add("$path:${index + 1}: ${line.trim()}")
                    }
                }
            }
        }

        assertTrue(
            residual.isEmpty(),
            "已迁移的 home 文件不能残留硬编码 shape/surface：\n" + residual.joinToString("\n")
        )
    }

    @Test
    fun migratedHomeMotionFiles_areNotInMotionAllowlist() {
        val leaked = migratedHomeMotionFiles.filter { it in StyleLintAllowlist.MOTION_HITS }
        assertTrue(
            leaked.isEmpty(),
            "已迁移的 home motion 文件不能继续留在 MOTION_HITS：\n" + leaked.joinToString("\n")
        )
    }

    @Test
    fun migratedHomeMotionFiles_haveNoResidualHardcodedMotion() {
        val motionPattern = Regex("""\b(tween|spring)\s*\(\s*\d+""")
        val residual = mutableListOf<String>()

        migratedHomeMotionFiles.forEach { path ->
            val file = locate(path) ?: error("Cannot locate $path from cwd")
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (motionPattern.containsMatchIn(line)) {
                        residual.add("$path:${index + 1}: ${line.trim()}")
                    }
                }
            }
        }

        assertTrue(
            residual.isEmpty(),
            "已迁移的 home motion 文件不能残留硬编码 tween/spring：\n" + residual.joinToString("\n")
        )
    }

    @Test
    fun homeFeature_wiresPresetTokens() {
        val homeRoot = locate("src/main/java/com/android/purebilibili/feature/home")
            ?: error("home dir not found")
        var appShapesCount = 0
        var appSurfaceTokensCount = 0

        homeRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                appShapesCount += Regex("""\bAppShapes\.container\(""").findAll(text).count()
                appSurfaceTokensCount += Regex("""\bAppSurfaceTokens\.""").findAll(text).count()
            }

        assertTrue(appShapesCount >= 4, "feature/home 至少应接入 4 处 AppShapes.container，当前 $appShapesCount")
        assertTrue(appSurfaceTokensCount >= 3, "feature/home 至少应接入 3 处 AppSurfaceTokens，当前 $appSurfaceTokensCount")
    }

    @Test
    fun bottomBarSegmentedControl_outerContainerUsesPresetPillShape() {
        val source = locate(
            "src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )?.readText() ?: error("BottomBarLiquidSegmentedControl source not found")

        assertTrue(
            source.contains("AppShapes.container(ContainerLevel.Pill)"),
            "底栏分段控件外层容器圆角应走 AppShapes Pill token"
        )
        assertFalse(
            source.contains("RoundedCornerShape(height / 2)"),
            "底栏分段控件外层容器不能继续按当前高度写死半径"
        )
    }

    private fun locate(path: String): File? {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull { it.exists() }
    }
}
