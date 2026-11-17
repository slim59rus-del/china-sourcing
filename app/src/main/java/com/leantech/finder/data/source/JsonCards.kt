package com.leantech.finder.data.source

import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.Seller
import org.json.JSONArray
import org.json.JSONObject

/**
 * Разбор ответов площадок.
 *
 * Серверы Taobao / 1688 / 闲鱼 устроены по одинаковой схеме (mtop): данные лежат в data -> список
 * карточек. Точные имена полей у площадок разные и время от времени меняются, поэтому разбор не
 * привязан к одному пути: он рекурсивно обходит JSON и собирает объекты, похожие на карточку
 * товара, по набору известных полей. Такой обход переживает переименование контейнеров.
 */
object JsonCards {

    private val TITLE_KEYS = listOf(
        "raw_title", "title", "itemTitle", "item_title", "subject", "offerTitle",
        "itemName", "productTitle",
    )
    private val ID_KEYS = listOf(
        "nid", "itemId", "item_id", "itemid", "itemID", "auctionId", "offerId",
        "offer_id", "targetId", "id",
    )
    private val PRICE_KEYS = listOf(
        "view_price", "price", "priceShow", "priceText", "currentPrice", "reservePrice",
        "soldPrice", "priceInfo", "promotionPrice", "consignPrice", "priceRange",
    )
    private val SALES_KEYS = listOf(
        "view_sales", "sold", "soldCount", "soldQuantity", "sales", "salesCount",
        "tradeNum", "trade_count", "wantCount", "wantNum", "collectCount", "soldCnt",
        "monthSold", "dealCount", "volume",
    )
    private val IMAGE_KEYS = listOf(
        "pic_url", "picUrl", "image", "imageUrl", "pic", "mainPic", "img",
        "coverUrl", "pic_path",
    )
    private val SELLER_NAME_KEYS = listOf(
        "nick", "sellerNick", "shopName", "companyName", "company", "sellerName",
        "loginId", "userNick", "memberId", "nickName", "userName", "seller_nick",
    )
    private val SHOP_URL_KEYS = listOf(
        "shopLink", "shopUrl", "shop_url", "sellerUrl", "companyUrl", "storeUrl",
        "homePage", "sellerUrlPC",
    )
    private val LOCATION_KEYS = listOf(
        "item_loc", "itemLoc", "location", "sellerLoc", "city", "province", "area",
        "companyAddress", "sellerLocation",
    )
    private val RATING_KEYS = listOf("sellerRate", "rating", "score", "creditLevel", "shopRate", "star")
    private val YEARS_KEYS = listOf("shopAge", "years", "openYears", "shopYears", "sellerYears")
    private val TAG_KEYS = listOf(
        "icons", "iconList", "tags", "tagList", "labels", "sellerTags", "badges",
        "shopTags", "serviceTags",
    )
    private val URL_KEYS = listOf("itemUrl", "detailUrl", "url", "link", "detail_url")

    /** Признаки фабрики/производителя в названии, метках и тегах магазина. */
    private val FACTORY_MARKERS = listOf(
        "工厂", "厂家", "厂商", "源头", "生产", "制造", "制造商", "加工厂", "实业",
        "工贸", "实力商家", "超级工厂", "factory", "manufacturer", "oem", "odm",
        "定制", "批发",
    )

    /** Признаки розничного магазина или посредника. */
    private val RESELLER_MARKERS = listOf(
        "旗舰店", "专营店", "专卖店", "代购", "转卖", "二手", "闲置", "个人",
    )

    fun extract(root: JSONObject, marketplace: Marketplace, query: String): List<Product> {
        val candidates = ArrayList<JSONObject>()
        walk(root, candidates, depth = 0)

        val seen = HashSet<String>()
        val products = ArrayList<Product>(candidates.size)

        for (node in candidates) {
            val product = runCatching { normalize(node, marketplace, query) }.getOrNull() ?: continue
            if (product.title.isBlank()) continue
            val key = product.id.ifBlank { product.title + product.priceText }
            if (!seen.add(key)) continue
            products.add(product)
        }
        return products
    }

    private fun walk(node: Any?, out: MutableList<JSONObject>, depth: Int) {
        if (depth > 12) return
        when (node) {
            is JSONObject -> {
                if (looksLikeCard(node)) {
                    out.add(node)
                    return
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    walk(node.opt(keys.next()), out, depth + 1)
                }
            }
            is JSONArray -> {
                for (index in 0 until node.length()) {
                    walk(node.opt(index), out, depth + 1)
                }
            }
        }
    }

    private fun looksLikeCard(node: JSONObject): Boolean {
        val title = firstString(node, TITLE_KEYS)
        if (title.isNullOrBlank()) return false
        val hasId = firstString(node, ID_KEYS) != null || firstString(node, URL_KEYS) != null
        val hasPrice = firstString(node, PRICE_KEYS) != null
        val hasSales = firstString(node, SALES_KEYS) != null
        return hasId || hasPrice || hasSales
    }

    private fun normalize(node: JSONObject, marketplace: Marketplace, query: String): Product {
        val id = firstString(node, ID_KEYS).orEmpty()
        val title = firstString(node, TITLE_KEYS).orEmpty().trim()
        val salesText = firstString(node, SALES_KEYS)
        val priceText = firstString(node, PRICE_KEYS).orEmpty()
        val sellerName = firstString(node, SELLER_NAME_KEYS).orEmpty()
        val tags = collectTags(node)
        val years = parseCount(firstString(node, YEARS_KEYS))
        val factorySignals = detectFactorySignals(
            title = title,
            sellerName = sellerName,
            tags = tags,
            salesText = salesText,
            marketplace = marketplace,
        )

        return Product(
            id = id,
            marketplace = marketplace,
            title = title,
            priceCny = parsePrice(priceText),
            priceText = priceText.trim(),
            salesCount = parseCount(salesText),
            salesText = salesText,
            itemUrl = itemUrl(marketplace, id, node, query),
            imageUrl = firstString(node, IMAGE_KEYS),
            seller = Seller(
                id = id.takeIf { it.isNotBlank() },
                name = sellerName,
                shopUrl = shopUrl(marketplace, node),
                location = firstString(node, LOCATION_KEYS),
                rating = firstString(node, RATING_KEYS),
                yearsOnPlatform = years,
                tags = tags,
                isFactory = factorySignals.isNotEmpty(),
                factorySignals = factorySignals,
            ),
        )
    }

    private fun detectFactorySignals(
        title: String,
        sellerName: String,
        tags: List<String>,
        salesText: String?,
        marketplace: Marketplace,
    ): List<String> {
        val haystack = (title + " " + sellerName + " " + tags.joinToString(" ") + " " +
            salesText.orEmpty()).lowercase()

        val reseller = RESELLER_MARKERS.filter { haystack.contains(it.lowercase()) }
        val signals = FACTORY_MARKERS.filter { haystack.contains(it.lowercase()) }
        if (reseller.isNotEmpty() && signals.isEmpty()) return emptyList()

        // 1688 по своей природе площадка оптовых фабрик и поставщиков: если прямых признаков нет,
        // предложение всё равно B2B, и об этом честно сообщается в описании.
        if (signals.isEmpty() && marketplace == Marketplace.ALIBABA_1688) {
            return listOf("оптовое предложение 1688")
        }
        return signals
    }

    private fun itemUrl(marketplace: Marketplace, id: String, node: JSONObject, query: String): String {
        firstString(node, URL_KEYS)?.let { direct ->
            if (direct.startsWith("http")) return direct
        }
        if (id.isBlank()) return marketplace.searchUrlTemplate.format(encode(query))
        return when (marketplace) {
            Marketplace.TAOBAO -> "https://item.taobao.com/item.htm?id=$id"
            Marketplace.ALIBABA_1688 -> "https://detail.1688.com/offer/$id.html"
            Marketplace.XIANYU -> "https://www.goofish.com/item?id=$id"
        }
    }

    private fun shopUrl(marketplace: Marketplace, node: JSONObject): String? {
        firstString(node, SHOP_URL_KEYS)?.let { direct ->
            if (direct.startsWith("http")) return direct
            if (direct.isNotBlank()) return "https://$direct"
        }
        val member = firstString(node, SELLER_NAME_KEYS)?.takeIf { it.isNotBlank() } ?: return null
        return when (marketplace) {
            Marketplace.TAOBAO ->
                if (member.length in 1..14) "https://shop$member.taobao.com/" else null
            Marketplace.ALIBABA_1688 ->
                if (member.length in 1..40) "https://$member.1688.com/" else null
            Marketplace.XIANYU -> null
        }
    }

    private fun collectTags(node: JSONObject): List<String> {
        val tags = LinkedHashSet<String>()
        for (key in TAG_KEYS) {
            val value = node.opt(key) ?: continue
            when (value) {
                is JSONArray -> for (index in 0 until value.length()) {
                    when (val entry = value.opt(index)) {
                        is String -> if (entry.isNotBlank()) tags.add(entry)
                        is JSONObject -> firstString(entry, listOf("text", "title", "name", "label", "iconText"))
                            ?.takeIf { it.isNotBlank() }?.let { tags.add(it) }
                        else -> Unit
                    }
                }
                is String -> if (value.isNotBlank()) tags.add(value)
                else -> Unit
            }
        }
        return tags.toList()
    }

    private fun firstString(node: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (!node.has(key)) continue
            when (val value = node.opt(key)) {
                is String -> if (value.isNotBlank()) return value.trim()
                is Number -> return value.toString()
                is Boolean -> return value.toString()
                is JSONArray -> {
                    val joined = StringBuilder()
                    for (index in 0 until value.length()) {
                        when (val entry = value.opt(index)) {
                            is String -> joined.append(entry)
                            is Number -> joined.append(entry)
                            is JSONObject -> firstString(entry, listOf("text", "title", "name", "value"))
                                ?.let { joined.append(it) }
                            else -> Unit
                        }
                        if (index < value.length() - 1) joined.append(' ')
                    }
                    val text = joined.toString().trim()
                    if (text.isNotBlank()) return text
                }
                is JSONObject -> firstString(value, listOf("text", "title", "name", "value", "price", "priceText"))
                    ?.takeIf { it.isNotBlank() }?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    /** «1.2万+人付款» -> 12000, «月销 500» -> 500, «500+» -> 500. */
    fun parseCount(raw: String?): Int? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val match = Regex("""(\d+(?:[.,]\d+)?)\s*(万|千|w|W|k|K)?""").find(text) ?: return null
        val number = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].lowercase()) {
            "万", "w" -> 10000.0
            "千", "k" -> 1000.0
            else -> 1.0
        }
        return (number * multiplier).toInt()
    }

    /** «¥12.50» -> 12.5, «от 5,80 юаней» -> 5.8, «12.5-18.0» -> 12.5. */
    fun parsePrice(raw: String?): Double? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val match = Regex("""(\d+(?:[.,]\d+)?)""").find(text) ?: return null
        return match.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
}
