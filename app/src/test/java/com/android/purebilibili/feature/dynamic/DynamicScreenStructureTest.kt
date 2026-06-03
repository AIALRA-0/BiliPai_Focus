package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicScreenStructureTest {

    @Test
    fun `dynamic staggered grid provides stable content types`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val gridSource = source
            .substringAfter("LazyVerticalStaggeredGrid(")
            .substringBefore("@Composable\nprivate fun OldContentDivider")

        assertTrue(gridSource.contains("contentType = \"dynamic_empty_state\""))
        assertTrue(gridSource.contains("contentType = { \"dynamic_card\" }"))
        assertTrue(gridSource.contains("contentType = \"dynamic_old_content_divider\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_loading_footer\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_no_more_footer\""))
    }
}
