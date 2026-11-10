package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModulesFlexibleSerializer
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.repository.articleContentBlocksToOpusBlocks
import com.android.purebilibili.feature.dynamic.components.resolveDynamicLinkCardDescriptions
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleGoodsCardsTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun paragraph(items: String): JsonObject = json.parseToJsonElement("""
        {"para_type":6,"link_card":{"card":{"type":"LINK_CARD_TYPE_GOODS","oid":"group-id",
          "goods":{"head_text":"商品推荐","jump_url":"https://mall.example/group","items":$items}
        }}}
    """).jsonObject

    @Test
    fun `all products retain order individual destinations and independent descriptions`() {
        val content = paragraph("""[
            {"id":"first","name":"商品甲","brief":"商品甲介绍","price":"¥19","cover":"//i0.hdslb.com/a.jpg","jump_url":"https://mall.example/a","jump_desc":"去看看"},
            {"id":"second","name":"商品乙","brief":"商品乙介绍","price":"¥29","cover":"//i0.hdslb.com/b.jpg","jump_url":"https://mall.example/b","jump_desc":"去购买"}
        ]""")
        val blocks = parseArticleContentBlocks(listOf(content), null)
        val cards = blocks.filterIsInstance<ArticleContentBlock.MediaCard>().map { it.card }
        assertEquals(listOf("first", "second"), cards.map { it.oid })
        assertEquals(listOf("商品甲", "商品乙"), cards.map { it.title })
        assertEquals(listOf("https://mall.example/a", "https://mall.example/b"), cards.map { it.jumpUrl })
        assertEquals(listOf("商品甲介绍", "¥19"), resolveDynamicLinkCardDescriptions(cards[0]))
        assertEquals(listOf("商品乙介绍", "¥29"), resolveDynamicLinkCardDescriptions(cards[1]))
        assertTrue(collectArticleBodyImageUrls(blocks).isEmpty())
        val restored = opusContentBlocksToArticleBlocks(articleContentBlocksToOpusBlocks(blocks))
        assertEquals(blocks, restored)
    }

    @Test
    fun `opus detail deserialization does not truncate the product group`() {
        val content = paragraph("""[
          {"id":"a","name":"商品甲","jump_url":"https://mall.example/a"},
          {"id":"b","name":"商品乙","jump_url":"https://mall.example/b"},
          {"id":"c","name":"商品丙","jump_url":"https://mall.example/c"}
        ]""")
        val item = json.decodeFromString<DynamicItem>("""{
            "id_str":"123","modules":[{"module_type":"MODULE_TYPE_CONTENT","module_content":{"paragraphs":[$content]}}]
        }""")
        val blocks = item.modules.module_dynamic?.major?.opus?.contentBlocks.orEmpty()
        val cards = blocks.filterIsInstance<OpusContentBlock.LinkCard>().map { it.card }
        assertEquals(listOf("a", "b", "c"), cards.map { it.oid })
        assertEquals(3, opusContentBlocksToArticleBlocks(blocks).size)
    }

    @Test
    fun `missing destination is not replaced by another item or group destination`() {
        val content = paragraph("""[
          {"name":"可打开","jump_url":"https://mall.example/a"},
          {"name":"无链接商品","brief":"仍保留介绍","price":"¥9","jump_desc":"购买"}
        ]""")
        val cards = DynamicModulesFlexibleSerializer.extractParagraphLinkCards(content)
        assertEquals("", cards[1].jumpUrl)
        assertEquals("", cards[1].badgeText)
        assertEquals(listOf("仍保留介绍", "¥9", "暂无商品链接"), resolveDynamicLinkCardDescriptions(cards[1]))
    }

    @Test
    fun `bad and empty entries do not hide later products or invent zero prices`() {
        val content = paragraph("""[
          null, 123, {}, {"name":{},"price":null}, {"name":false,"price":true},
          {"name":"零价商品","price":0,"jump_url":"https://mall.example/zero"},
          {"name":"价格未知","price":"undefined","brief":"正常介绍","jump_url":"https://mall.example/unknown"}
        ]""")
        val cards = DynamicModulesFlexibleSerializer.extractParagraphLinkCards(content)
        assertEquals(listOf("零价商品", "价格未知"), cards.map { it.title })
        assertEquals("0", cards[0].goodsItem?.price)
        assertEquals("", cards[1].goodsItem?.price)
        assertEquals(listOf("正常介绍"), resolveDynamicLinkCardDescriptions(cards[1]))
        assertTrue(parseArticleContentBlocks(listOf(paragraph("[]")), null).isEmpty())
        assertTrue(parseArticleContentBlocks(listOf(paragraph("null")), null).isEmpty())
    }
}
