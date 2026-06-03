package com.android.purebilibili.feature.plugin

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AdFilterPluginPolicyTest {

    @Test
    fun customListSummary_describesEmptyList() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "0 个",
                previewText = "暂无拉黑的UP主",
                hiddenCountText = null
            ),
            resolveAdFilterCustomListSummary(
                items = emptyList(),
                emptyText = "暂无拉黑的UP主"
            )
        )
    }

    @Test
    fun customListSummary_showsSmallListInline() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "2 个",
                previewText = "UP甲、UP乙",
                hiddenCountText = null
            ),
            resolveAdFilterCustomListSummary(
                items = listOf("UP甲", "UP乙"),
                emptyText = "暂无拉黑的UP主"
            )
        )
    }

    @Test
    fun customListSummary_reportsHiddenCountForLongList() {
        assertEquals(
            AdFilterCustomListSummary(
                countText = "5 个",
                previewText = "广告、推广、恰饭",
                hiddenCountText = "还有 2 个，展开查看全部"
            ),
            resolveAdFilterCustomListSummary(
                items = listOf("广告", "推广", "恰饭", "抽奖", "福利"),
                emptyText = "暂无自定义屏蔽词"
            )
        )
    }

    @Test
    fun customListVisibleItems_respectsExpandedState() {
        val items = listOf("广告", "推广", "恰饭", "抽奖", "福利")

        assertEquals(
            listOf("广告", "推广", "恰饭"),
            resolveAdFilterCustomListVisibleItems(items, expanded = false)
        )
        assertEquals(
            items,
            resolveAdFilterCustomListVisibleItems(items, expanded = true)
        )
    }

    @Test
    fun removeCustomListItem_removesOnlyMatchingEntries() {
        assertEquals(
            listOf("广告", "推广"),
            removeAdFilterCustomListItem(
                items = listOf("广告", "恰饭", "推广", "恰饭"),
                item = "恰饭"
            )
        )
    }

    @Test
    fun buildAdFilterRecord_capturesVideoCoverAndUpFace() {
        val record = buildAdFilterRecord(
            item = videoItem(
                title = "本视频由品牌赞助",
                cover = "https://cover.example/1.jpg",
                upName = "测试UP",
                upFace = "https://face.example/up.jpg",
                viewCount = 888
            ),
            reasonType = AdFilterReasonType.SPONSORED,
            matchedText = "赞助",
            timestampMs = 1_700_000_000_000L
        )

        assertEquals("本视频由品牌赞助", record.videoTitle)
        assertEquals("https://cover.example/1.jpg", record.videoCoverUrl)
        assertEquals("测试UP", record.upName)
        assertEquals("https://face.example/up.jpg", record.upFaceUrl)
        assertEquals("广告推广", record.reasonLabel)
        assertEquals("赞助", record.matchedText)
        assertEquals(888, record.viewCount)
    }

    @Test
    fun resolveAdFilterInsightSummary_groupsRecentRecordsByReason() {
        val records = listOf(
            adFilterRecord(AdFilterReasonType.SPONSORED, "广告", timestampMs = 10_000L),
            adFilterRecord(AdFilterReasonType.CLICKBAIT, "震惊", timestampMs = 12_000L),
            adFilterRecord(AdFilterReasonType.LOW_VIEW, "20 播放", timestampMs = 11_000L),
            adFilterRecord(AdFilterReasonType.CUSTOM_KEYWORD, "抽奖", timestampMs = 13_000L)
        )

        val summary = resolveAdFilterInsightSummary(
            records = records,
            blockedUpNames = listOf("UP甲")
        )

        assertEquals(4, summary.totalFilteredCount)
        assertEquals(1, summary.blockedUpCount)
        assertEquals(listOf("广告"), summary.sponsoredRecords.map { it.matchedText })
        assertEquals(listOf("震惊"), summary.clickbaitRecords.map { it.matchedText })
        assertEquals(listOf("20 播放"), summary.lowViewRecords.map { it.matchedText })
        assertEquals(listOf("抽奖"), summary.customKeywordRecords.map { it.matchedText })
    }

    @Test
    fun resolveAdFilterBlockedUpProfiles_usesLatestAvatarFromFilteredRecords() {
        val records = listOf(
            adFilterRecord(
                reasonType = AdFilterReasonType.BLOCKED_UP,
                matchedText = "UP甲",
                upName = "UP甲",
                upFace = "old-face",
                upMid = 1L,
                timestampMs = 10_000L
            ),
            adFilterRecord(
                reasonType = AdFilterReasonType.BLOCKED_UP,
                matchedText = "UP甲",
                upName = "UP甲",
                upFace = "new-face",
                upMid = 1L,
                timestampMs = 12_000L
            )
        )

        val profiles = resolveAdFilterBlockedUpProfiles(
            records = records,
            blockedUpNames = listOf("UP甲", "UP乙")
        )

        assertEquals("new-face", profiles[0].faceUrl)
        assertEquals(2, profiles[0].filteredCount)
        assertEquals("", profiles[1].faceUrl)
        assertEquals(0, profiles[1].filteredCount)
    }

    @Test
    fun resolveAdFilterBlockedUpProfiles_usesCachedAvatarWhenRecordHasNoFace() {
        val records = listOf(
            adFilterRecord(
                reasonType = AdFilterReasonType.BLOCKED_UP,
                matchedText = "UP甲",
                upName = "UP甲",
                upFace = "",
                upMid = 42L,
                timestampMs = 10_000L
            )
        )

        val profiles = resolveAdFilterBlockedUpProfiles(
            records = records,
            blockedUpNames = listOf("UP甲"),
            cachedUpProfiles = listOf(
                AdFilterUpProfile(
                    name = "UP甲",
                    faceUrl = "cached-face",
                    mid = 42L,
                    updatedAtMs = 12_000L
                )
            )
        )

        assertEquals("cached-face", profiles[0].faceUrl)
        assertEquals(42L, profiles[0].mid)
        assertEquals(1, profiles[0].filteredCount)
    }

    @Test
    fun resolveAdFilterRecordUpFaceUrl_fallsBackToCachedProfile() {
        val record = adFilterRecord(
            reasonType = AdFilterReasonType.CUSTOM_KEYWORD,
            matchedText = "华为",
            upName = "数码UP",
            upFace = "",
            upMid = 88L,
            timestampMs = 10_000L
        )

        assertEquals(
            "cached-face",
            resolveAdFilterRecordUpFaceUrl(
                record = record,
                upProfiles = listOf(
                    AdFilterUpProfile(
                        name = "数码UP",
                        faceUrl = "cached-face",
                        mid = 88L,
                        updatedAtMs = 12_000L
                    )
                )
            )
        )
    }

    private fun adFilterRecord(
        reasonType: AdFilterReasonType,
        matchedText: String,
        upName: String = "UP",
        upFace: String = "face",
        upMid: Long = 1L,
        timestampMs: Long
    ): AdFilterRecord {
        return buildAdFilterRecord(
            item = videoItem(
                title = "视频-$timestampMs",
                cover = "cover-$timestampMs",
                upName = upName,
                upFace = upFace,
                upMid = upMid
            ),
            reasonType = reasonType,
            matchedText = matchedText,
            timestampMs = timestampMs
        )
    }

    private fun videoItem(
        title: String,
        cover: String,
        upName: String,
        upFace: String,
        upMid: Long = 1L,
        viewCount: Int = 100
    ): VideoItem {
        return VideoItem(
            bvid = "BV1",
            cid = 2L,
            title = title,
            pic = cover,
            owner = Owner(mid = upMid, name = upName, face = upFace),
            stat = Stat(view = viewCount)
        )
    }
}
