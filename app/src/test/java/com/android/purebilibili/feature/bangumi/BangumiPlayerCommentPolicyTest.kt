package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BangumiPlayerCommentPolicyTest {

    @Test
    fun `bangumi player content exposes intro and comment as first level tabs`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt"
        )

        assertTrue(source.contains("tabs = listOf(\"简介\""))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("VideoCommentMainList("))
    }

    @Test
    fun `bangumi player screen initializes comments for current episode aid`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiPlayerScreen.kt"
        )

        assertTrue(source.contains("commentViewModel.init("))
        assertTrue(source.contains("aid = currentAid"))
        assertTrue(source.contains("expectedReplyCount = successState?.seasonDetail?.stat?.reply?.toInt() ?: 0"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
