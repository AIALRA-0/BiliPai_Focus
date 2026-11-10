package com.android.purebilibili.feature.video.share

import java.io.File
import com.android.purebilibili.data.model.response.FollowingUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoSharePolicyTest {

    @Test
    fun buildVideoSharePayload_outputsTitleUrlAndText() {
        val payload = buildVideoSharePayload(
            title = " Uzi回应送老婆贵价项链 ",
            bvid = " BV1aRG46aEnz ",
            coverUrl = " https://i0.hdslb.com/bfs/archive/test.jpg ",
            upName = " 鹿乃ちゃん ",
            playCountText = " 36.3万 ",
        )

        assertEquals("Uzi回应送老婆贵价项链", payload.title)
        assertEquals("BV1aRG46aEnz", payload.bvid)
        assertEquals("https://i0.hdslb.com/bfs/archive/test.jpg", payload.coverUrl)
        assertEquals("https://www.bilibili.com/video/BV1aRG46aEnz", payload.url)
        assertEquals(
            "【Uzi回应送老婆贵价项链】\nhttps://www.bilibili.com/video/BV1aRG46aEnz",
            payload.text
        )
        assertEquals("鹿乃ちゃん", payload.upName)
        assertEquals("36.3万", payload.playCountText)
        assertEquals("UP主：鹿乃ちゃん  ·  播放：36.3万", resolveVideoShareCardMetaLine(payload))
    }

    @Test
    fun resolveVideoShareCardMetaLine_omitsMissingFields() {
        assertEquals(
            "UP主：测试",
            resolveVideoShareCardMetaLine(
                buildVideoSharePayload(title = "标题", bvid = "BV1", upName = "测试")
            )
        )
        assertEquals(
            "",
            resolveVideoShareCardMetaLine(
                buildVideoSharePayload(title = "标题", bvid = "BV1")
            )
        )
    }

    @Test
    fun videoShareStyle_defaultsToLinkAndCard() {
        assertEquals(VideoShareStyle.LINK, VideoShareStyle.valueOf("LINK"))
        assertEquals(VideoShareStyle.CARD, VideoShareStyle.valueOf("CARD"))
    }

    @Test
    fun videoShareTarget_mapsWechatAndQqPackages() {
        assertNull(VideoShareTarget.BILIBILI_FRIENDS.packageName)
        assertEquals("com.tencent.mm", VideoShareTarget.WECHAT.packageName)
        assertEquals("com.tencent.mobileqq", VideoShareTarget.QQ.packageName)
        assertNull(VideoShareTarget.COPY_LINK.packageName)
        assertNull(VideoShareTarget.MORE.packageName)
    }

    @Test
    fun shareRecipientsContainOnlySelectedFollowedPeopleOnce() {
        val followings = listOf(
            FollowingUser(mid = 21, uname = "甲"),
            FollowingUser(mid = 22, uname = "乙"),
            FollowingUser(mid = 21, uname = "甲"),
            FollowingUser(mid = 0),
        )

        assertEquals(
            listOf(21L, 22L),
            resolveVideoShareRecipientIds(setOf(21L, 22L, 99L, 0L), followings, selfMid = 99L),
        )
        assertEquals(
            listOf(21L),
            resolveVideoShareRecipientIds(setOf(21L, 22L), followings, selfMid = 22L),
        )
    }

    @Test
    fun buildVideoShareIntent_usesOrdinaryPlainTextActionSend() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("Intent(Intent.ACTION_SEND)"),
            "More share should use ordinary ACTION_SEND"
        )
        assertTrue(
            source.contains("""type = "text/plain""""),
            "Video share intents should use text/plain"
        )
        assertTrue(
            source.contains("putExtra(Intent.EXTRA_SUBJECT, payload.title)"),
            "Video share intents should include title as subject"
        )
        assertTrue(
            source.contains("putExtra(Intent.EXTRA_TEXT, payload.text)"),
            "Video share intents should include unified share text"
        )
    }

    @Test
    fun buildVideoCoverShareIntent_attachesCoverImageWithoutBilibiliBrandText() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("buildVideoCoverShareIntent"),
            "Video sharing should support a cover image stream"
        )
        assertTrue(
            source.contains("putExtra(Intent.EXTRA_STREAM, coverUri)"),
            "Cover sharing should attach the downloaded video cover uri"
        )
        assertTrue(
            source.contains("clipData = ClipData.newUri"),
            "Cover sharing should grant the receiving app read access to the cover uri"
        )
        assertTrue(
            source.contains("addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)"),
            "Cover sharing should grant temporary read permission"
        )
        val coverShareIntentBody = source
            .substringAfter("internal fun buildVideoCoverShareIntent")
            .substringBefore("\n}")
        assertTrue(
            coverShareIntentBody.contains("putExtra(Intent.EXTRA_TITLE, payload.title)"),
            "Card share should expose the video title for host apps"
        )
        assertTrue(
            coverShareIntentBody.contains("putExtra(Intent.EXTRA_SUBJECT, payload.title)"),
            "Card share should expose the video subject for host apps"
        )
        assertTrue(
            coverShareIntentBody.contains("putExtra(Intent.EXTRA_TEXT, payload.url)"),
            "Card share should attach link-only text so recipients can jump"
        )
        assertTrue(
            coverShareIntentBody.contains("clipData = ClipData.newUri"),
            "Card share clip label should use the video title"
        )
        assertTrue(
            coverShareIntentBody.contains("payload.title, coverUri"),
            "Card share clip label should use the video title"
        )
        assertTrue(
            !source.contains("哔哩哔哩"),
            "Generated cover share intent should not inject a bottom-left Bilibili brand label"
        )
    }

    @Test
    fun resolveVideoShareCardFileName_prefersSanitizedTitle() {
        assertEquals(
            "BiliPai_share_card_Uzi回应送老婆贵价项链.jpg",
            resolveVideoShareCardFileName(
                buildVideoSharePayload(title = "Uzi回应送老婆贵价项链", bvid = "BV1aRG46aEnz")
            )
        )
        assertEquals(
            "BiliPai_share_card_鹿乃_翻唱《_我、和我们_》.jpg",
            resolveVideoShareCardFileName(
                buildVideoSharePayload(title = "鹿乃 翻唱《 我、和我们 》", bvid = "BV1")
            )
        )
    }

    @Test
    fun buildVideoCoverShareIntent_usesLinkOnlyExtraText() {
        val source = loadVideoSharePolicySource()
        val coverShareIntentBody = source
            .substringAfter("internal fun buildVideoCoverShareIntent")
            .substringBefore("\n}")

        assertTrue(
            coverShareIntentBody.contains("putExtra(Intent.EXTRA_TEXT, payload.url)"),
            "Card share EXTRA_TEXT should be the bare link"
        )
        assertTrue(
            !coverShareIntentBody.contains("putExtra(Intent.EXTRA_TEXT, payload.text)"),
            "Card share EXTRA_TEXT should not repeat the bracketed title"
        )
    }

    @Test
    fun buildTargetedShareIntent_setsTargetPackage() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("setPackage(packageName)"),
            "Targeted WeChat/QQ sharing should constrain the ACTION_SEND intent to the target package"
        )
    }

    @Test
    fun buildTargetedShareIntent_pinsFriendShareComponentWhenResolved() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("setComponent(ComponentName(packageName, activityClassName))"),
            "Targeted WeChat/QQ sharing should pin the friend-share activity component"
        )
    }

    @Test
    fun resolvePreferredShareActivity_prefersQqFriendOverUtilities() {
        val picked = resolvePreferredShareActivity(
            listOf(
                ShareActivityCandidate(
                    packageName = QQ_PACKAGE_NAME,
                    className = "com.tencent.mobileqq.activity.FavoriteActivity",
                    label = "保存到QQ收藏",
                ),
                ShareActivityCandidate(
                    packageName = QQ_PACKAGE_NAME,
                    className = "com.tencent.mobileqq.activity.JumpActivity",
                    label = "发送给好友",
                ),
                ShareActivityCandidate(
                    packageName = QQ_PACKAGE_NAME,
                    className = "com.tencent.mobileqq.activity.QfileJumpActivity",
                    label = "发送到我的电脑",
                ),
                ShareActivityCandidate(
                    packageName = QQ_PACKAGE_NAME,
                    className = "com.tencent.mobileqq.flash.FlashTransferActivity",
                    label = "QQ闪传·大文件无损传",
                ),
            )
        )

        assertEquals("发送给好友", picked?.label)
        assertEquals("com.tencent.mobileqq.activity.JumpActivity", picked?.className)
    }

    @Test
    fun resolvePreferredShareActivity_prefersWeChatSendToFriend() {
        val picked = resolvePreferredShareActivity(
            listOf(
                ShareActivityCandidate(
                    packageName = WECHAT_PACKAGE_NAME,
                    className = "com.tencent.mm.ui.tools.AddFavoriteUI",
                    label = "添加到微信收藏",
                ),
                ShareActivityCandidate(
                    packageName = WECHAT_PACKAGE_NAME,
                    className = "com.tencent.mm.ui.tools.ShareImgUI",
                    label = "发送给朋友",
                ),
            )
        )

        assertEquals("com.tencent.mm.ui.tools.ShareImgUI", picked?.className)
    }

    @Test
    fun resolvePreferredShareActivity_returnsNullWhenOnlyExcludedEntries() {
        assertNull(
            resolvePreferredShareActivity(
                listOf(
                    ShareActivityCandidate(
                        packageName = WECHAT_PACKAGE_NAME,
                        className = "com.tencent.mm.ui.tools.AddFavoriteUI",
                        label = "添加到微信收藏",
                    ),
                )
            )
        )
    }

    @Test
    fun scoreShareActivityCandidate_excludesFavoriteTimelineAndFlashEntries() {
        assertEquals(
            0,
            scoreShareActivityCandidate(
                ShareActivityCandidate(QQ_PACKAGE_NAME, "com.tencent.mobileqq.activity.FavoriteActivity", "保存到QQ收藏")
            )
        )
        assertEquals(
            0,
            scoreShareActivityCandidate(
                ShareActivityCandidate(WECHAT_PACKAGE_NAME, "com.tencent.mm.ui.tools.SendToTimeLineUI", "分享到朋友圈")
            )
        )
        assertEquals(
            0,
            scoreShareActivityCandidate(
                ShareActivityCandidate(QQ_PACKAGE_NAME, "com.tencent.mobileqq.flash.FlashTransferActivity", "QQ闪传")
            )
        )
    }

    private fun loadVideoSharePolicySource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/share/VideoSharePolicy.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/share/VideoSharePolicy.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoSharePolicy.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
