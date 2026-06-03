package com.android.purebilibili.feature.video.share

import java.io.File
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
            coverUrl = " https://i0.hdslb.com/bfs/archive/test.jpg "
        )

        assertEquals("Uzi回应送老婆贵价项链", payload.title)
        assertEquals("BV1aRG46aEnz", payload.bvid)
        assertEquals("https://i0.hdslb.com/bfs/archive/test.jpg", payload.coverUrl)
        assertEquals("https://www.bilibili.com/video/BV1aRG46aEnz", payload.url)
        assertEquals(
            "【Uzi回应送老婆贵价项链】\nhttps://www.bilibili.com/video/BV1aRG46aEnz",
            payload.text
        )
    }

    @Test
    fun videoShareTarget_mapsWechatAndQqPackages() {
        assertEquals("com.tencent.mm", VideoShareTarget.WECHAT.packageName)
        assertEquals("com.tencent.mobileqq", VideoShareTarget.QQ.packageName)
        assertNull(VideoShareTarget.COPY_LINK.packageName)
        assertNull(VideoShareTarget.MORE.packageName)
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
            !coverShareIntentBody.contains("putExtra(Intent.EXTRA_TEXT"),
            "Cover sharing should not include text extras because WeChat/QQ render that as a text bubble"
        )
        assertTrue(
            !source.contains("哔哩哔哩"),
            "Generated cover share intent should not inject a bottom-left Bilibili brand label"
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
