package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.DynamicAdditionalReserve
import com.android.purebilibili.data.model.response.DynamicCardButton
import com.android.purebilibili.data.model.response.DynamicCardButtonStyle
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.feature.dynamic.components.LINK_CARD_TYPE_RESERVE
import com.android.purebilibili.feature.dynamic.components.resolveDynamicReserveUi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.assertIs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleMediaCardInteractionPolicyTest {
    @Test
    fun `unknown numeric content presents separate explicit destinations`() {
        val targets = resolveArticleUnavailableTargets("123")
        assertEquals(listOf("视频", "动态 / 图文", "专栏"), targets.map { it.label })
        assertEquals(listOf(
            "https://www.bilibili.com/video/av123",
            "https://www.bilibili.com/opus/123",
            "https://www.bilibili.com/read/cv123",
        ), targets.map { it.url })
    }

    @Test
    fun `known prefixes cannot become unrelated resource types`() {
        assertEquals(listOf("专栏"), resolveArticleUnavailableTargets("CV123").map { it.label })
        assertEquals(listOf("视频"), resolveArticleUnavailableTargets("av123").map { it.label })
        assertEquals(listOf("视频"), resolveArticleUnavailableTargets("BV1xx411c7mD").map { it.label })
        listOf("", "0", "-1", "undefined", "123/script", "99999999999999999999999").forEach {
            assertTrue(resolveArticleUnavailableTargets(it).isEmpty(), it)
        }
    }

    @Test
    fun `missing content identity does not produce copy placeholders`() {
        listOf("", "  ", "null", "undefined", "0").forEach { assertNull(resolveArticleUnavailableContentId(it)) }
        assertEquals("cv123", resolveArticleUnavailableContentId(" cv123 "))
    }

    @Test
    fun `reservation states explain ended missing and disabled actions`() {
        val card = OpusLinkCard(type = LINK_CARD_TYPE_RESERVE,
            reserve = DynamicAdditionalReserve(rid = 7, button = DynamicCardButton(
                type = 1, status = 0, uncheck = DynamicCardButtonStyle(text = "预约"),
            )))
        assertNull(resolveArticleReserveNotice(card, "123"))
        assertEquals("当前专栏暂不支持直接预约", resolveArticleReserveNotice(card, ""))
        assertEquals("预约已结束", resolveArticleReserveNotice(card.copy(reserve = card.reserve!!.copy(state = -1)), "123"))
        assertEquals("预约信息暂不可用", resolveArticleReserveNotice(card.copy(reserve = null), "123"))
        assertEquals("预约信息暂不可用", resolveArticleReserveNotice(card.copy(reserve = card.reserve!!.copy(rid = 0)), "123"))
        val disabled = card.copy(reserve = card.reserve!!.copy(button = card.reserve!!.button!!.copy(
            uncheck = DynamicCardButtonStyle(text = "预约", disable = 1, toast = "预约已截止"),
        )))
        assertEquals("预约已截止", resolveArticleReserveNotice(disabled, "123"))
        assertNull(resolveArticleReserveNotice(disabled.copy(reserve = disabled.reserve!!.copy(button = disabled.reserve!!.button!!.copy(
            jump_url = "https://live.bilibili.com/123",
        ))), ""))
    }
    @Test
    fun `structured article cards retain reservation actions and unavailable identities`() {
        val reserveParagraph = Json.parseToJsonElement("""{"para_type":6,"link_card":{"card":{
          "type":"LINK_CARD_TYPE_RESERVE","oid":"7","reserve":{
            "rid":"7","title":"首播预约","desc3":{"text":"查看详情","jump_url":"https://live.bilibili.com/123"},
            "button":{"type":1,"status":0,"check":{"text":"已预约"},"uncheck":{"text":"预约"}}
          }
        }}}""").jsonObject
        val reserved = assertIs<ArticleContentBlock.MediaCard>(parseArticleContentBlocks(listOf(reserveParagraph), null).single())
        val action = resolveDynamicReserveUi(reserved.card, "123456")!!
        assertEquals(7L, action.action.reserveId)
        assertEquals("123456", action.action.dynamicId)
        assertEquals("https://live.bilibili.com/123", action.descriptionJumpUrl)
        val unavailableParagraph = Json.parseToJsonElement("""{"para_type":6,"link_card":{"card":{
          "type":"LINK_CARD_TYPE_ITEM_NULL","oid":"170001","item_null":{"text":"稿件已删除"}
        }}}""").jsonObject
        val unavailable = assertIs<ArticleContentBlock.MediaCard>(parseArticleContentBlocks(listOf(unavailableParagraph), null).single())
        assertEquals("稿件已删除", unavailable.card.title)
        assertEquals(3, resolveArticleUnavailableTargets(unavailable.card.oid).size)
        assertTrue(collectArticleBodyImageUrls(listOf(unavailable)).isEmpty())
    }

}
