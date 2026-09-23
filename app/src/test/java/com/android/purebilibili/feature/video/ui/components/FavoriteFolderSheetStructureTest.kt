package com.android.purebilibili.feature.video.ui.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteFolderSheetStructureTest {

    @Test
    fun `favorite folder sheet constrains sheet height and keeps footer outside the list`() {
        val source = loadFavoriteFolderSheetSource()

        assertTrue(
            "sheet content should cap its height so the footer stays visible",
            source.contains(".heightIn(max = maxSheetHeight)")
        )
        assertTrue(
            "folder list should occupy the remaining height and scroll internally",
            source.contains("modifier = Modifier.weight(1f)")
        )
        assertTrue(
            "footer should reserve navigation bar space instead of being pushed out of view",
            source.contains(".navigationBarsPadding()")
        )
    }

    @Test
    fun `create folder action does not overlay the multi-select subtitle`() {
        val source = loadFavoriteFolderSheetSource()

        assertTrue(
            "title should reserve horizontal space so 新建 cannot cover it",
            source.contains(".padding(horizontal = 56.dp)")
        )
        assertTrue(
            "create action should stay on the title row end",
            source.contains("AppText(\"新建\")") &&
                source.contains("Modifier.align(Alignment.CenterEnd)")
        )
        assertTrue(
            "subtitle should use full-width centered layout on its own line",
            source.contains("可勾选一个或多个收藏夹，将视频收藏到自己的收藏夹") &&
                source.contains("textAlign = TextAlign.Center")
        )
        // Old layout put title+subtitle in one centered Column while 新建 was CenterEnd overlay.
        assertFalse(
            "do not center a title+subtitle Column under the create action overlay",
            source.contains("horizontalAlignment = Alignment.CenterHorizontally")
        )
    }

    private fun loadFavoriteFolderSheetSource(): String {
        val workspaceRoot = generateSequence(
            Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        ) { current ->
            current.parent
        }.first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/video/ui/components/FavoriteFolderSheet.kt"
                )
            )
        }
        return workspaceRoot.resolve(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/FavoriteFolderSheet.kt"
        ).readText()
    }
}
