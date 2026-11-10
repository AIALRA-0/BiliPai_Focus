package com.android.purebilibili.feature.share

import com.android.purebilibili.core.network.grpc.ProtoWire
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.buildDynamicShareSendRequest
import com.android.purebilibili.feature.video.share.buildVideoSharePayload
import com.android.purebilibili.feature.video.share.withPosition
import com.android.purebilibili.feature.video.ui.components.resolveReplyCommentShareUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ContentSharePolicyTest {
    @Test
    fun preciseShareKeepsCurrentPartAndUsesOnlyRequestedPosition() {
        val payload = buildVideoSharePayload("视频", "BV1", page = 3, positionMs = 125600)
        assertEquals("https://www.bilibili.com/video/BV1?p=3", payload.withPosition(false).url)
        assertEquals("https://www.bilibili.com/video/BV1?p=3&t=125.6", payload.withPosition(true).url)
        assertTrue(payload.withPosition(true).text.endsWith("?p=3&t=125.6"))
        assertEquals("https://www.bilibili.com/video/BV1", buildVideoSharePayload("视频", "BV1", page = -1, positionMs = -1).withPosition(true).url)
    }

    @Test
    fun commentUrlsPreserveSourceAndSecondaryReply() {
        val reply = ReplyItem(oid = 11, root = 22, rpid = 33, replyType = 17)
        assertTrue(resolveReplyCommentShareUrl(reply).startsWith("bilibili://comment/detail/17/11/22/?anchor=33&enterUri="))
        assertTrue(resolveReplyCommentShareUrl(reply.copy(replyType = 1), ReplyShareContext("https://www.bilibili.com/bangumi/play/ep99")).contains("enterUri=bilibili%3A%2F%2Fpgc%2Fseason%2Fep%2F99"))
        assertTrue(resolveReplyCommentShareUrl(reply.copy(replyType = 1), ReplyShareContext("https://www.bilibili.com/video/BV1?p=4", page = 4)).endsWith("?p=4&comment_on=1&comment_root_id=22&comment_secondary_id=33"))
        assertTrue(resolveReplyCommentShareUrl(reply.copy(replyType = 12)).startsWith("bilibili://comment/detail/12/11/22/"))
    }

    @Test
    fun commentDocumentIncludesAllTextPicturesAndEmotes() {
        val text = (1..100).joinToString("\n") { "第 $it 行[微笑]" }
        val document = replyShareDocument(ReplyItem(
            oid = 1, rpid = 2,
            content = ReplyContent(message = text, emote = mapOf("[微笑]" to ReplyEmote(url = "emote")), pictures = listOf(ReplyPicture(imgSrc = "picture"))),
        ))
        val body = document.blocks.filterIsInstance<ShareImageBlock.Text>().first()
        assertEquals(text, body.text)
        assertEquals("emote", body.emotes["[微笑]"])
        assertEquals(listOf("picture"), document.blocks.filterIsInstance<ShareImageBlock.Image>().map { it.url })
    }

    @Test
    fun dynamicDocumentIncludesOriginalAndAllOrderedContentBlocks() {
        val original = DynamicItem(id_str = "original", modules = DynamicModules(module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "原文"))))
        val item = DynamicItem(id_str = "outer", orig = original, modules = DynamicModules(module_dynamic = DynamicContentModule(
            major = DynamicMajor(opus = OpusMajor(contentBlocks = listOf(
                OpusContentBlock.Text("首段"), OpusContentBlock.Image(OpusPic(url = "picture")), OpusContentBlock.Text("末段"),
            ))),
        )))
        val blocks = dynamicShareDocument(item).blocks
        assertTrue(blocks.indexOf(ShareImageBlock.Text("首段")) < blocks.indexOf(ShareImageBlock.Image("picture")))
        assertTrue(blocks.indexOf(ShareImageBlock.Image("picture")) < blocks.indexOf(ShareImageBlock.Text("末段")))
        assertTrue(blocks.contains(ShareImageBlock.Text("原文")))
    }

    @Test
    fun mentionIdentitySurvivesEditingAndNamesDoNotMatchPrefixes() {
        val content = buildShareRichContents("你好 @小明 同学 @小明同学 ", mapOf("小明" to 42L))
        assertEquals("你好 @小明 同学 @小明同学 ", content.joinToString("") { it.raw_text })
        assertEquals(listOf("42"), content.filter { it.type == 2 }.map { it.biz_id })
    }

    @Test
    fun liveUsesCommonCardProtocolAndRealRoomIdentity() {
        val payload = buildLiveSharePayload(99L, "直播标题", "cover", "主播", 42L)
        val card = Json.parseToJsonElement(payload.messageCard!!).jsonObject
        assertEquals("99", card.getValue("sourceID").jsonPrimitive.content)
        assertEquals("42", card.getValue("authorID").jsonPrimitive.content)
        assertEquals(14, payload.messageType)
        val bytes = buildDynamicShareSendRequest(1L, 2L, requireNotNull(payload.messageCard), 3L, "device", messageType = payload.messageType)
        val message = ProtoWire.parseFields(bytes).first { it.number == 1 }.bytes
        assertEquals(14L, ProtoWire.parseFields(message).first { it.number == 5 }.varint)
    }

    @Test
    fun repostKeepsMentionAndTopicResourceIdentity() {
        val contents = buildShareRichContents("@作者 一起看", mapOf("作者" to 42L))
        val request = com.android.purebilibili.core.network.buildDynamicRepostRequest(
            dynamicId = "123", content = "@作者 一起看", richContents = contents,
            topic = DynamicCreateTopic(99L, "讨论"),
        )
        assertEquals("123", request.web_repost_src.dyn_id_str)
        assertEquals(99L, request.dyn_req.topic?.id)
        assertEquals("42", request.dyn_req.content.contents.first { it.type == 2 }.biz_id)
        assertTrue(buildShareRichContents("", emptyMap()).isEmpty())
    }

    @Test
    fun bangumiResourceTypeMatchesSeasonCategory() {
        assertEquals(listOf(4097, 4098, 4101, 4100, 4099, 4099), listOf(1, 2, 3, 4, 5, 7).map(::bangumiDynamicType))
        assertNull(bangumiDynamicType(10))
    }

    @Test
    fun bangumiShareKeepsEpisodeIdentityAndOptionalPlaybackPosition() {
        val payload = buildBangumiContentSharePayload(
            epId = 99L, title = "剧集", cover = "cover", seasonType = 1,
            positionMs = 125_600L, aid = 42L,
        )
        assertEquals("https://www.bilibili.com/bangumi/play/ep99", payload.url)
        assertEquals("https://www.bilibili.com/bangumi/play/ep99?t=125.6", buildPositionShareUrl(payload.url, payload.positionMs))
        assertEquals(99L, payload.resourceId)
        assertEquals(42L, payload.replyOid)
        assertEquals(4097, payload.dynamicType)
    }
}
