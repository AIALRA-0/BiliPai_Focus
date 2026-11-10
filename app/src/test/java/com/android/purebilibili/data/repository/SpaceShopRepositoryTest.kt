package com.android.purebilibili.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class SpaceShopRepositoryTest {
    @Test
    fun `shop fields preserve price currency benefit and source`() {
        val page = parseSpaceShopPage(Json.parseToJsonElement("""{
            "data":[{"title":"商品","cover":{"url":"https://example.com/a.png"},
            "cardUrl":"https://example.com/product","netPrice":{"pricePrefix":"券后","priceSymbol":"¥","netPrice":"19.90"},
            "belowLabels":[{"title":"包邮"}],"benefitInfos":[{"prefix":"已售","amount":"10","suffix":"件"}],"itemSourceName":"小店"}],
            "showMoreTab":false
        }""").jsonObject)
        assertEquals("券后 ¥19.90", page.products.single().price)
        assertEquals("包邮", page.products.single().labels)
        assertEquals("已售10件", page.products.single().benefits)
        assertEquals("小店", page.products.single().source)
        assertNull(page.moreUrl)
        assertEquals("查看更多", page.moreLabel)
    }

    @Test
    fun `empty shop and missing optional product fields are supported`() {
        assertTrue(parseSpaceShopPage(Json.parseToJsonElement("{}").jsonObject).products.isEmpty())
        val page = parseSpaceShopPage(Json.parseToJsonElement("""{"data":[{"title":"商品"}]}""").jsonObject)
        assertEquals("", page.products.single().price)
        assertEquals("", page.products.single().cover)
    }
}
