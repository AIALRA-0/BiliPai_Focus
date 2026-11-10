package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcEpisodeArc
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

class PlayerEpisodeMenuPolicyTest {
    @Test
    fun `portrait panel stays attached to safe bottom when measured shorter`() {
        assertEquals(
            IntOffset(0, 500),
            resolvePlayerPanelPopupPosition(
                IntRect(0, 400, 400, 1000), IntSize(400, 1020), IntSize(400, 500),
                bottomAligned = true,
            ),
        )
        assertEquals(
            IntOffset(0, 450),
            resolvePlayerPanelPopupPosition(
                IntRect(0, 400, 400, 1000), IntSize(400, 950), IntSize(400, 500),
                bottomAligned = true,
            ),
        )
    }

    @Test
    fun `panel popup retains its safe right edge in the normal window`() {
        assertEquals(
            IntOffset(680, 10),
            resolvePlayerPanelPopupPosition(
                IntRect(680, 10, 1180, 590), IntSize(1200, 600), IntSize(500, 580),
            ),
        )
    }

    @Test
    fun `smaller popup window keeps trailing controls inside its actual right edge`() {
        val size = IntSize(500, 560)
        val window = IntSize(1120, 580)
        val position = resolvePlayerPanelPopupPosition(IntRect(680, 10, 1180, 590), window, size)
        assertEquals(IntOffset(620, 10), position)
        assertTrue(position.x + size.width <= window.width)
        assertTrue(position.y + size.height <= window.height)
    }

    @Test
    fun `constrained content stays right aligned and above the popup bottom`() {
        assertEquals(
            IntOffset(880, 0),
            resolvePlayerPanelPopupPosition(
                IntRect(680, 20, 1180, 600), IntSize(1200, 400), IntSize(300, 400),
            ),
        )
        assertEquals(
            IntOffset.Zero,
            resolvePlayerPanelPopupPosition(
                IntRect(0, 0, 500, 500), IntSize.Zero, IntSize.Zero,
            ),
        )
    }

    @Test
    fun `parts take priority and callbacks retain zero based source index rather than page number`() {
        val pages = listOf(
            Page(cid = 31L, page = 3, part = "开场", duration = 73L),
            Page(cid = 71L, page = 7, part = "结尾", duration = 94L),
        )
        val content = resolvePlayerEpisodeMenuContent(
            pages, 1, season(UgcEpisode(bvid = "collection", cid = 91L)), "collection", 91L,
        )

        assertEquals(1, content.sections.size)
        assertEquals(listOf("P3", "P7"), content.sections.single().entries.map { it.indexLabel })
        assertEquals(listOf(0, 1), content.sections.single().entries.map {
            (it.target as PlayerEpisodeMenuTarget.Part).pageIndex
        })
        assertEquals(94L, content.sections.single().entries[1].durationSeconds)
        assertEquals(0, content.currentSectionIndex)
        assertEquals(1, content.currentEntryIndex)
    }

    @Test
    fun `invalid current page never highlights a different part`() {
        val content = resolvePlayerEpisodeMenuContent(
            listOf(Page(cid = 1), Page(cid = 2)), 9, null, "", 0L,
        )
        assertEquals(-1, content.currentSectionIndex)
        assertEquals(-1, content.currentEntryIndex)
        assertEquals(2, content.sections.single().entries.size)
    }

    @Test
    fun `single page falls through to collection and empty sections do not hide later options`() {
        val episode = UgcEpisode(id = 9L, bvid = "BV9", cid = 90L, title = "最后一集")
        val content = resolvePlayerEpisodeMenuContent(
            listOf(Page(cid = 1)), 0,
            UgcSeason(sections = listOf(UgcSection(), UgcSection(title = "下篇", episodes = listOf(episode)))),
            "BV9", 90L,
        )

        assertEquals(listOf("下篇"), content.sections.map { it.title })
        assertEquals(0, content.currentSectionIndex)
    }

    @Test
    fun `collection parts remain individually selectable with all episode identity preserved`() {
        val episode = UgcEpisode(
            id = 2L, aid = 222L, bvid = "BVparts", cid = 11L, title = "课程",
            arc = UgcEpisodeArc(duration = 100),
            pages = listOf(
                Page(cid = 11L, page = 1, part = "前半", duration = 40),
                Page(cid = 12L, page = 2, part = "后半", duration = 60),
            ),
        )
        val content = resolvePlayerEpisodeMenuContent(emptyList(), 0, season(episode), "BVparts", 12L)
        val entries = content.sections.single().entries

        assertEquals(listOf("1 · P1", "1 · P2"), entries.map { it.indexLabel })
        assertEquals(listOf("课程 · 前半", "课程 · 后半"), entries.map { it.title })
        assertEquals(listOf(40L, 60L), entries.map { it.durationSeconds })
        assertEquals(episode.copy(cid = 12L), (entries[1].target as PlayerEpisodeMenuTarget.Episode).episode)
        assertEquals(1, content.currentEntryIndex)
    }

    @Test
    fun `exact bvid and cid across sections wins over earlier bvid and equal cid on another video`() {
        val content = resolvePlayerEpisodeMenuContent(
            emptyList(), 0,
            UgcSeason(sections = listOf(
                UgcSection(title = "前篇", episodes = listOf(UgcEpisode(bvid = "BVsame", cid = 1))),
                UgcSection(title = "后篇", episodes = listOf(
                    UgcEpisode(bvid = "BVother", cid = 2),
                    UgcEpisode(bvid = "BVsame", cid = 2),
                )),
            )),
            "BVsame", 2L,
        )

        assertEquals(1, content.currentSectionIndex)
        assertEquals(1, content.currentEntryIndex)
        assertEquals(3, content.sections.sumOf { it.entries.size })
    }

    @Test
    fun `unknown cid falls back to exactly one first matching video row`() {
        val content = resolvePlayerEpisodeMenuContent(
            emptyList(), 0,
            season(UgcEpisode(bvid = "BVsame", pages = listOf(Page(cid = 10), Page(cid = 20)))),
            "BVsame", 0L,
        )
        assertEquals(0, content.currentSectionIndex)
        assertEquals(0, content.currentEntryIndex)
    }

    @Test
    fun `single part target uses its cid and missing part cid preserves episode target`() {
        val first = UgcEpisode(bvid = "BVfirst", cid = 10, pages = listOf(Page(cid = 12)))
        val second = UgcEpisode(bvid = "BVsecond", cid = 20, pages = listOf(Page(cid = 0)))
        val content = resolvePlayerEpisodeMenuContent(emptyList(), 0, season(first, second), "", 0)
        val targets = content.sections.single().entries.map {
            (it.target as PlayerEpisodeMenuTarget.Episode).episode
        }
        assertEquals(listOf(first.copy(cid = 12), second), targets)
        assertEquals(-1, content.currentSectionIndex)
        assertEquals(-1, content.currentEntryIndex)
    }

    @Test
    fun `long collection retains every entry and absent playlist data produces no invented options`() {
        val episodes = List(100) { index -> UgcEpisode(bvid = "BV$index", cid = index.toLong() + 1) }
        val content = resolvePlayerEpisodeMenuContent(emptyList(), 0, season(*episodes.toTypedArray()), "BV99", 100)
        assertEquals(episodes, content.sections.single().entries.map {
            (it.target as PlayerEpisodeMenuTarget.Episode).episode
        })
        assertEquals(99, content.currentEntryIndex)
        assertEquals(emptyList(), resolvePlayerEpisodeMenuContent(listOf(Page()), 0, null, "", 0).sections)
    }

    @Test
    fun `panel covers only remaining portrait detail region inline`() {
        val safe = androidx.compose.ui.unit.IntRect(10, 24, 410, 880)
        val player = androidx.compose.ui.unit.IntRect(10, 24, 410, 300)
        assertEquals(androidx.compose.ui.unit.IntRect(10, 300, 410, 880),
            resolvePlayerPanelBounds(safe, player, false, false, 500))
    }

    @Test
    fun `panel covers only right hand detail region landscape inline`() {
        val safe = androidx.compose.ui.unit.IntRect(20, 10, 1180, 590)
        val player = androidx.compose.ui.unit.IntRect(20, 10, 780, 590)
        assertEquals(androidx.compose.ui.unit.IntRect(780, 10, 1180, 590),
            resolvePlayerPanelBounds(safe, player, false, true, 500))
    }

    @Test
    fun `fullscreen landscape uses half width capped at 500 and entire safe height`() {
        val safe = androidx.compose.ui.unit.IntRect(20, 10, 1180, 590)
        assertEquals(androidx.compose.ui.unit.IntRect(680, 10, 1180, 590),
            resolvePlayerPanelBounds(safe, null, true, true, 500))
        assertEquals(androidx.compose.ui.unit.IntRect(400, 10, 780, 590),
            resolvePlayerPanelBounds(androidx.compose.ui.unit.IntRect(20, 10, 780, 590), null, true, true, 500))
    }

    @Test
    fun `fullscreen portrait uses bottom seventy percent and inline without detail room falls back identically`() {
        val safe = androidx.compose.ui.unit.IntRect(0, 20, 400, 820)
        val expected = androidx.compose.ui.unit.IntRect(0, 260, 400, 820)
        assertEquals(expected, resolvePlayerPanelBounds(safe, null, true, false, 500))
        assertEquals(expected, resolvePlayerPanelBounds(safe, safe, false, false, 500))
    }

    @Test
    fun `available detail room is never replaced by overlapping fallback sheet`() {
        val safe = androidx.compose.ui.unit.IntRect(0, 20, 400, 820)
        assertEquals(androidx.compose.ui.unit.IntRect(0, 800, 400, 820),
            resolvePlayerPanelBounds(safe, androidx.compose.ui.unit.IntRect(0, 20, 400, 800), false, false, 500))
    }

    @Test
    fun `part thumbnail and episode fallback cover are retained`() {
        val parts = resolvePlayerEpisodeMenuContent(
            listOf(Page(firstFrame = "frame"), Page()), 0, null, "", 0)
        assertEquals("frame", parts.sections.single().entries.first().cover)
        val collection = resolvePlayerEpisodeMenuContent(emptyList(), 0,
            UgcSeason(cover = "season-cover", sections = listOf(UgcSection(episodes = listOf(
                UgcEpisode(bvid = "BV", pages = listOf(Page(cid = 1))),
            )))), "BV", 1)
        assertEquals("season-cover", collection.sections.single().entries.single().cover)
    }

    @Test
    fun `multipart videos retain a single cover row and individually addressable nested targets`() {
        val first = UgcEpisode(bvid = "BVparts", cid = 11, title = "课程",
            pages = listOf(Page(cid = 11, page = 1), Page(cid = 12, page = 2)))
        val second = UgcEpisode(bvid = "BVnext", cid = 21)
        val content = resolvePlayerEpisodeMenuContent(emptyList(), 0, season(first, second), "BVparts", 12)
        val groups = groupPlayerEpisodeEntries(content.sections.single().entries)
        assertEquals(listOf(2, 1), groups.map { it.size })
        assertEquals(listOf(11L, 12L), groups.first().map {
            (it.target as PlayerEpisodeMenuTarget.Episode).episode.cid
        })
        assertEquals(listOf("BVparts", "BVparts"), groups.first().map {
            (it.target as PlayerEpisodeMenuTarget.Episode).episode.bvid
        })
    }

    private fun season(vararg episodes: UgcEpisode) = UgcSeason(
        sections = listOf(UgcSection(title = "合集", episodes = episodes.toList())),
    )
}
