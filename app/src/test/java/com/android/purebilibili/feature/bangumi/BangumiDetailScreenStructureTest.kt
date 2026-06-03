package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class BangumiDetailScreenStructureTest {

    @Test
    fun `bangumi detail top bar does not use haze blur`() {
        val source = File("app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiDetailScreen.kt")
            .takeIf { it.exists() }
            ?: File("src/main/java/com/android/purebilibili/feature/bangumi/BangumiDetailScreen.kt")
        val text = source.readText()

        assertFalse(text.contains("Modifier.unifiedBlur"))
        assertFalse(text.contains("hazeSource(state"))
        assertFalse(text.contains("rememberRecoverableHazeState"))
    }
}
