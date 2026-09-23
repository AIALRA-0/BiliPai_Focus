package com.android.purebilibili.feature.live

import com.android.purebilibili.core.ui.lint.StyleLintAllowlist
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveTokenAdoptionStructureTest {
    private val liveRoot = sourceFile("src/main/java/com/android/purebilibili/feature/live")

    @Test
    fun live_secondary_pages_use_adaptive_chrome_and_shared_room_card() {
        val secondaryPages = listOf(
            "LiveFollowingScreen.kt",
            "LiveAreaScreen.kt",
            "LiveAreaDetailScreen.kt",
            "LiveSearchScreen.kt",
        )
        secondaryPages.forEach { fileName ->
            val source = File(liveRoot, fileName).readText()
            assertTrue(source.contains("AppScaffold("), "$fileName must use AppScaffold")
            assertTrue(source.contains("AppTopBar("), "$fileName must use AppTopBar")
        }

        val sharedCard = File(liveRoot, "LiveRoomCard.kt").readText()
        assertTrue(sharedCard.contains("data class LiveRoomCardUiModel"))
        assertTrue(sharedCard.contains("internal fun LiveRoomCard("))
        listOf("LiveListScreen.kt", "LiveFollowingScreen.kt", "LiveAreaDetailScreen.kt", "LiveSearchScreen.kt")
            .forEach { fileName ->
                assertTrue(File(liveRoot, fileName).readText().contains("LiveRoomCard("))
            }
    }

    @Test
    fun live_grids_share_columns_and_shell_bottom_padding() {
        listOf("LiveListScreen.kt", "LiveFollowingScreen.kt", "LiveAreaDetailScreen.kt", "LiveSearchScreen.kt")
            .forEach { fileName ->
                val source = File(liveRoot, fileName).readText()
                assertTrue(source.contains("resolveLiveBiliPaiGridColumns("), "$fileName column policy")
                assertTrue(source.contains("windowSizeClass.isTablet"), "$fileName tablet layout")
                assertTrue(source.contains("LocalBottomBarContentPadding.current"), "$fileName bottom padding")
            }

        val areaSource = File(liveRoot, "LiveAreaScreen.kt").readText()
        assertTrue(areaSource.contains("windowSizeClass.isTablet"))
        assertTrue(areaSource.contains("responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)"))
        assertTrue(areaSource.contains("bottom = LocalBottomBarContentPadding.current"))
    }

    @Test
    fun live_player_orientation_uses_display_context() {
        val source = File(liveRoot, "LivePlayerScreen.kt").readText()
        assertTrue(source.contains("resolveLiveRequestedOrientationMode("))
        assertTrue(source.contains("displayContext = displayContext"))
        assertTrue(source.contains("rememberSaveable { mutableStateOf(false) }"))
        assertFalse(source.contains("LaunchedEffect(isTablet, isFullscreen)"))
    }

    @Test
    fun live_player_visible_back_leaves_without_exposing_split_chat() {
        val source = File(liveRoot, "LivePlayerScreen.kt").readText()
        val exitBlock = source
            .substringAfter("fun exitLiveRoom() {")
            .substringBefore("fun captureLiveScreenshot()")

        assertTrue(exitBlock.contains("markLeavingByNavigation(forceStop = true)"))
        assertTrue(exitBlock.contains("onBack()"))
        assertFalse(exitBlock.contains("toggleFullscreen()"))
        assertTrue(source.contains("else if (isFullscreen) toggleFullscreen()"))
    }

    @Test
    fun live_area_detail_renders_room_summary_in_all_content_states() {
        val source = File(liveRoot, "LiveAreaDetailScreen.kt").readText()

        assertTrue(source.contains("val roomSummary = buildString"))
        assertTrue(source.contains("text = roomSummary"))
        assertFalse(source.contains("subtitle = buildString"))
    }

    @Test
    fun live_area_filters_remain_available_before_empty_and_error_states() {
        val source = File(liveRoot, "LiveAreaDetailScreen.kt").readText()
        val summaryIndex = source.indexOf("text = roomSummary")
        val filterIndex = source.indexOf("LazyRow(", startIndex = summaryIndex)
        val stateIndex = source.indexOf("when {", startIndex = summaryIndex)

        assertTrue(summaryIndex >= 0)
        assertTrue(filterIndex in (summaryIndex + 1) until stateIndex)
    }

    @Test
    fun live_search_footer_spans_the_full_adaptive_grid() {
        val source = File(liveRoot, "LiveSearchScreen.kt").readText()

        assertTrue(source.contains("item(span = { GridItemSpan(maxLineSpan) })"))
    }

    @Test
    fun shared_live_room_card_uses_preset_aware_minimum_details_height() {
        val source = File(liveRoot, "LiveRoomCard.kt").readText()

        assertTrue(source.contains("resolveLiveVisualSpec("))
        assertTrue(source.contains("heightIn(min = visualSpec.roomCardDetailsMinHeightDp.dp)"))
    }

    @Test
    fun responsive_width_is_applied_before_fill_constraints() {
        val invalidOrder = Regex(
            """\.fillMax(?:Size|Width)\(\)\s*\.responsiveContentWidth\(""",
        )
        listOf(
            "LiveListScreen.kt",
            "LiveFollowingScreen.kt",
            "LiveAreaScreen.kt",
            "LiveAreaDetailScreen.kt",
            "LiveSearchScreen.kt",
        ).forEach { fileName ->
            val source = File(liveRoot, fileName).readText()
            assertFalse(invalidOrder.containsMatchIn(source), "$fileName width modifier order")
        }
    }

    @Test
    fun migrated_live_files_are_not_in_legacy_style_allowlists() {
        val livePrefix = "src/main/java/com/android/purebilibili/feature/live/"
        assertFalse(StyleLintAllowlist.SHAPE_HITS.any { it.startsWith(livePrefix) })
        assertFalse(StyleLintAllowlist.SURFACE_HITS.any { it.startsWith(livePrefix) })
        assertFalse(StyleLintAllowlist.MOTION_HITS.any { it.startsWith(livePrefix) })
    }

    private fun sourceFile(relativePath: String): File {
        return listOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull(File::exists)
            ?: error("Cannot find $relativePath from ${File(".").absolutePath}")
    }
}
