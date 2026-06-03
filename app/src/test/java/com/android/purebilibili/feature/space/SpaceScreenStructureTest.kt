package com.android.purebilibili.feature.space

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpaceScreenStructureTest {

    @Test
    fun `contribution videos render as grid cards instead of full width rows`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("columns = GridCells.Fixed("))
        assertTrue(source.contains("resolveSpaceContentGridColumnCount("))
        assertTrue(source.contains("SpaceContributionVideoLayoutMode.GRID"))
        assertTrue(source.contains("SpaceHomeVideoCard("))
        assertTrue(source.contains("resolveSpaceContributionVideoItemKey("))
        assertFalse(source.contains("SpaceVideoListItemRow("))
    }

    @Test
    fun `contribution videos switch layout without dual placing lazy grid content`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")
        val contributionVideoItems = source
            .substringAfter("items(\n                            items = state.videos")
            .substringBefore("if (state.isLoadingMore)")

        assertTrue(source.contains("onLayoutModeClick"))
        assertTrue(source.contains("toggleSpaceContributionVideoLayoutMode"))
        assertTrue(source.contains("resolveSpaceContributionVideoGridSpan("))
        assertTrue(source.contains("resolveSpaceContributionVideoItemKey("))
        assertTrue(source.contains("SpaceContributionVideoLayoutMode.SINGLE_COLUMN"))
        assertTrue(source.contains("SpaceArchiveListItemRow("))
        assertFalse(contributionVideoItems.contains("Modifier.animateItem()"))
        assertFalse(contributionVideoItems.contains("AnimatedContent("))
        assertFalse(contributionVideoItems.contains("SizeTransform("))
    }

    @Test
    fun `space high frequency video covers join shared element transition`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("sharedTransitionKey = resolveSpaceArchiveSharedTransitionKey(video.bvid)"))
        assertTrue(source.contains("sharedTransitionKey = resolveSpaceArchiveSharedTransitionKey(topVideo.bvid)"))
        assertTrue(source.contains("sharedTransitionKey = resolveSpaceArchiveSharedTransitionKey(item.bvid)"))
        assertTrue(source.contains("CardPositionManager.recordVideoCardPosition("))
        assertTrue(source.contains("videoCoverSharedElementKey("))
        assertTrue(source.contains("clipInOverlayDuringTransition = OverlayClip(coverShape)"))
    }

    @Test
    fun `contribution screen uses compact toolbar instead of separate tab and action rows`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("SpaceContributionToolbar("))
        assertFalse(source.contains("SpaceContributionTabRow("))
        assertFalse(source.contains("SpaceContributionVideoActions("))
        assertTrue(source.contains("resolveSpaceContributionToolbarSpec("))
    }

    @Test
    fun `contribution toolbar long press expands full horizontal tab rail`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("SpaceContributionToolbarDock("))
        assertTrue(source.contains("SpaceContributionCollapsedTab("))
        assertTrue(source.contains("SpaceContributionExpandedTabRail("))
        assertTrue(source.contains("Surface("))
        assertTrue(source.contains("AppShapes.container(ContainerLevel.Pill)"))
        assertTrue(source.contains(".combinedClickable("))
        assertTrue(source.contains("onExpand = { expanded = true }"))
        assertTrue(source.contains("onLongClick = onExpand"))
        assertTrue(source.contains("horizontalScroll(scrollState)"))
        assertTrue(source.contains("rememberTextMeasurer()"))
        assertTrue(source.contains("minimumTouchTargetSize.width"))
        assertTrue(source.contains("textMeasurer.measure("))
        assertTrue(source.contains("val tabWidths = remember("))
        assertTrue(source.contains("tabs.forEachIndexed"))
        assertTrue(source.contains(".width(tabWidths.getOrElse(index) { minimumTouchTargetWidth })"))
        assertTrue(source.contains("if (expanded) {"))
        assertTrue(source.contains("} else {\n                    SpaceContributionCollapsedTab("))
        assertTrue(source.contains("if (toolbarSpec.collapseAfterTabSelection) expanded = false"))
        assertFalse(source.contains("AnimatedVisibility(visible = expanded)"))
        assertFalse(
            source.contains(
                """
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(toolbarSpec.tabHeightDp.dp)
                ) {
                    if (expanded) {
                """.trimIndent()
            )
        )
    }

    @Test
    fun `space search action scrolls to focused search bar and dynamic body opens comments`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")

        assertTrue(source.contains("resolveSpaceSearchBarGridItemIndex("))
        assertTrue(source.contains("resolveSpaceSearchBarRevealScrollOffsetPx("))
        assertTrue(source.contains("scrollOffset = searchBarRevealScrollOffsetPx"))
        assertTrue(source.contains("val searchFocusRequester = remember { FocusRequester() }"))
        assertTrue(source.contains(".focusRequester(searchFocusRequester)"))
        assertTrue(source.contains("onPrimaryClickOverride = { onSpaceDynamicCommentClick(dynamic) }"))
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
