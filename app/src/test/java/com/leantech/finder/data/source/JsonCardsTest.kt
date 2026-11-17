package com.leantech.finder.data.source

import com.leantech.finder.domain.model.Marketplace
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonCardsTest {

    @Test
    fun `счётчик продаж читается вместе с множителем 万`() {
        assertEquals(12000, JsonCards.parseCount("1.2万+人付款"))
        assertEquals(500, JsonCards.parseCount("月销 500"))
        assertEquals(3000, JsonCards.parseCount("3000+"))
        assertEquals(1_500_000, JsonCards.parseCount("150万 人付款"))
    }

    @Test
    fun `цена вытаскивается из текста`() {
        assertEquals(12.5, JsonCards.parsePrice("¥12.50"))
        assertEquals(5.8, JsonCards.parsePrice("от 5,80 юаней"))
        assertEquals(12.5, JsonCards.parsePrice("12.5-18.0"))
    }

    @Test
    fun `карточка обычного ответа превращается в товар`() {
        val root = JSONObject(
            """
            {"data":{"mods":{"itemlist":{"data":{"auctions":[
              {"nid":"7","raw_title":"厂家直销 折叠椅","view_price":"38.80",
               "view_sales":"1.2万+人付款","item_loc":"广东 佛山","nick":"佛山家具工厂店",
               "pic_url":"https://img/1.jpg"}
            ]}}}}}
            """.trimIndent()
        )

        val products = JsonCards.extract(root, Marketplace.TAOBAO, "складной стул")
        assertEquals(1, products.size)
        val product = products.first()
        assertEquals("厂家直销 折叠椅", product.title)
        assertEquals(38.8, product.priceCny!!, 0.001)
        assertEquals(12000, product.salesCount)
        assertEquals("广东 佛山", product.seller.location)
        assertTrue(product.seller.isFactory)
        assertEquals("https://item.taobao.com/item.htm?id=7", product.itemUrl)
    }

    @Test
    fun `розничный магазин не помечается фабрикой`() {
        val root = JSONObject(
            """
            {"data":{"items":[
              {"itemId":"9","title":"二手 手机 旗舰店","price":"99","sold":"10"}
            ]}}
            """.trimIndent()
        )
        val products = JsonCards.extract(root, Marketplace.XIANYU, "телефон")
        assertEquals(1, products.size)
        assertEquals(false, products.first().seller.isFactory)
    }
}
