package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.feature.dynamic.components.hasDynamicCommentSortIndicatorScaleClearance
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCommentSortControlSpec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicCommentSortControlPolicyTest {

    @Test
    fun `sort segmented control matches beta36 dynamic comment geometry`() {
        val spec = resolveDynamicCommentSortControlSpec(itemCount = 2)

        assertEquals(66, spec.itemWidthDp)
        assertEquals(40, spec.heightDp)
        assertEquals(30, spec.indicatorHeightDp)
        assertTrue(
            hasDynamicCommentSortIndicatorScaleClearance(
                containerHeightDp = spec.heightDp,
                indicatorHeightDp = spec.indicatorHeightDp,
            )
        )
    }

    @Test
    fun `sort segmented control reuses bottom-bar liquid glass with tap press`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        )
        val rendererSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicAdaptiveSegmentedControl.kt"
        )

        assertTrue(source.contains("DynamicAdaptiveSegmentedControl("))
        assertTrue(rendererSource.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(rendererSource.contains("tapPressRefractionEnabled = true"))
        assertTrue(rendererSource.contains("liquidGlassEffectsEnabled = true"))
        assertTrue(!rendererSource.contains("forceLiquidChrome"))
        assertTrue(source.contains("itemWidth = spec.itemWidthDp.dp"))
        assertTrue(source.contains("requiredWidth((spec.itemWidthDp * items.size).dp)"))
        assertTrue(source.contains("contentAlignment = Alignment.CenterEnd"))
        assertTrue(source.contains("listOf(CommentSortMode.HOT, CommentSortMode.NEWEST)"))
        assertTrue(!source.contains("CommentSegmentedControl("))
    }

    @Test
    fun `vote and publish exclusive switches reuse compact liquid glass`() {
        val voteSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCreateVoteDialog.kt"
        )
        val publishSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicPublishComposer.kt"
        )

        assertTrue(voteSource.contains("DynamicAdaptiveSegmentedControl("))
        assertTrue(voteSource.contains("listOf(\"单选\", \"多选\")"))
        assertTrue(voteSource.contains("backdrop = voteChromeBackdrop"))
        assertTrue(voteSource.contains("if (voteChromeBackdrop != null)"))
        assertTrue(voteSource.contains(".layerBackdrop(voteChromeBackdrop)"))
        assertTrue(
            voteSource.indexOf(".layerBackdrop(voteChromeBackdrop)") <
                voteSource.indexOf("DynamicAdaptiveSegmentedControl(")
        )
        assertTrue(publishSource.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(publishSource.contains("listOf(\"公开\", \"仅自己可见\")"))
        assertTrue(publishSource.contains("tapPressRefractionEnabled = true"))
        assertTrue(!voteSource.contains("AppFilterChip("))
        assertTrue(!publishSource.contains("AppFilterChip("))
    }

    @Test
    fun `comment sheet samples list content from a sibling backdrop`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        )

        assertTrue(source.contains("val commentChromeBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains(".layerBackdrop(commentChromeBackdrop)"))
        assertTrue(source.contains("miuixBackdrop = commentChromeBackdrop"))
        assertTrue(
            source.indexOf(".layerBackdrop(commentChromeBackdrop)") <
                source.indexOf("miuixBackdrop = commentChromeBackdrop")
        )
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
