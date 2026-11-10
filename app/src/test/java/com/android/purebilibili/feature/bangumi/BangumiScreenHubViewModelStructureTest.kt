package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 任务17 结构测试：首页 Bangumi/Cinema hub 必须使用 BangumiHubViewModel（不得回退到旧 BangumiViewModel），
 * 且 hub UI 只采集单一 uiState。
 */
class BangumiScreenHubViewModelStructureTest {

    private val source = loadSource(
        "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiScreen.kt"
    )

    @Test
    fun `hub screen uses BangumiHubViewModel`() {
        assertTrue(
            source.contains("viewModel: BangumiHubViewModel = viewModel()"),
            "BangumiScreen must default to BangumiHubViewModel"
        )
        assertFalse(
            Regex("""viewModel\s*:\s*BangumiViewModel\s*=\s*viewModel\(\)""").containsMatchIn(source),
            "BangumiScreen must not fall back to the legacy BangumiViewModel"
        )
    }

    @Test
    fun `hub screen collects a single uiState`() {
        assertTrue(source.contains("val state by viewModel.uiState.collectAsStateWithLifecycle()"))
        assertFalse(
            Regex("""viewModel\.\w+State\.collectAsStateWithLifecycle\(\)""").containsMatchIn(source),
            "hub must not collect per-field StateFlows"
        )
    }
}

private fun loadSource(path: String): String {
    val normalizedPath = path.removePrefix("app/")
    val sourceFile = listOf(
        File(path),
        File(normalizedPath),
        File("app", normalizedPath)
    ).firstOrNull { it.exists() }
    require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
    return sourceFile.readText()
}
