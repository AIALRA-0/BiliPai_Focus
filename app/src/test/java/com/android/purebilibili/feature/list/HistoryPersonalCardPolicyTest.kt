package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryPersonalCardPolicyTest {

    @Test
    fun progressLabel_distinguishesCompletedCurrentAndDurationOnly() {
        assertEquals("已看完", resolveHistoryProgressLabel(-1, 120))
        assertEquals("00:30/02:00", resolveHistoryProgressLabel(30, 120))
        assertEquals("02:00", resolveHistoryProgressLabel(0, 120))
    }

    @Test
    fun kindLabel_matchesHistoryBusiness() {
        assertEquals("视频", resolveHistoryKindLabel(HistoryBusiness.ARCHIVE))
        assertEquals("番剧", resolveHistoryKindLabel(HistoryBusiness.PGC))
        assertEquals("直播", resolveHistoryKindLabel(HistoryBusiness.LIVE))
        assertEquals("专栏", resolveHistoryKindLabel(HistoryBusiness.ARTICLE))
    }

    @Test
    fun watchLater_isOnlyAvailableForArchiveWithAid() {
        assertTrue(canAddHistoryToWatchLater(HistoryItem(VideoItem(id = 42), HistoryBusiness.ARCHIVE)))
        assertFalse(canAddHistoryToWatchLater(HistoryItem(VideoItem(id = 42), HistoryBusiness.PGC)))
        assertFalse(canAddHistoryToWatchLater(HistoryItem(VideoItem(id = 0), HistoryBusiness.ARCHIVE)))
    }

    @Test
    fun historyCoverMatchesPiliPlusSixteenByTenAndShadowDuration() {
        val source = listOf(
            File("src/main/java/com/android/purebilibili/feature/list/HistoryPersonalCard.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/list/HistoryPersonalCard.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO"))
        assertTrue(source.contains("resolveVideoCardCoverOverlayTextShadow()"))
        assertTrue(source.contains("FeedTitleHierarchy.Standard"))
        assertTrue(source.contains("maxLines = titleMaxLines"))
        assertFalse(source.contains("VideoStatRow("))
        assertTrue(source.contains("HorizontalVideoCardFrame("))
        assertTrue(source.contains("PersonalMediaCardFrame("))
        assertFalse(source.contains("MediaContrastPalette.Scrim.copy(alpha = 0.76f)"))
        assertFalse(source.contains("overlineContent"))
    }
}
