package com.leantech.finder.data.translate

/**
 * Офлайн-страховка: перевод по словарю частых категорий и товарных слов плюс транслитерация
 * остатка в латиницу. Работает без сети и используется, когда онлайн-перевод недоступен,
 * чтобы на площадку всё равно ушёл осмысленный запрос.
 */
class GlossaryTranslator : Translator {

    override suspend fun toChinese(text: String): String? {
        val words = text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val joined = words.joinToString("") { word -> DICTIONARY[word] ?: transliterate(word) }
        return joined.ifBlank { null }
    }

    override suspend fun toRussian(text: String): String? = null

    private fun transliterate(word: String): String = buildString {
        for (char in word) {
            when (char) {
                'а' -> append("a"); 'б' -> append("b"); 'в' -> append("v"); 'г' -> append("g")
                'д' -> append("d"); 'е' -> append("e"); 'ё' -> append("e"); 'ж' -> append("zh")
                'з' -> append("z"); 'и' -> append("i"); 'й' -> append("i"); 'к' -> append("k")
                'л' -> append("l"); 'м' -> append("m"); 'н' -> append("n"); 'о' -> append("o")
                'п' -> append("p"); 'р' -> append("r"); 'с' -> append("s"); 'т' -> append("t")
                'у' -> append("u"); 'ф' -> append("f"); 'х' -> append("h"); 'ц' -> append("c")
                'ч' -> append("ch"); 'ш' -> append("sh"); 'щ' -> append("shch"); 'ъ' -> append("")
                'ы' -> append("y"); 'ь' -> append(""); 'э' -> append("e"); 'ю' -> append("yu")
                'я' -> append("ya")
                else -> append(char)
            }
        }
    }

    private companion object {
        val DICTIONARY = mapOf(
            "телефон" to "手机", "смартфон" to "智能手机", "чехол" to "手机壳", "наушники" to "耳机",
            "часы" to "手表", "игрушка" to "玩具", "игрушки" to "玩具", "одежда" to "服装",
            "футболка" to "T恤", "куртка" to "外套", "обувь" to "鞋", "кроссовки" to "运动鞋",
            "сумка" to "包", "рюкзак" to "背包", "лампа" to "灯", "светильник" to "灯具",
            "мебель" to "家具", "стол" to "桌子", "стул" to "椅子", "кровать" to "床",
            "кухня" to "厨房", "посуда" to "餐具", "инструмент" to "工具", "дрель" to "电钻",
            "запчасть" to "配件", "деталь" to "零件", "кабель" to "数据线", "зарядка" to "充电器",
            "повербанк" to "充电宝", "колонка" to "音箱", "камера" to "摄像头", "монитор" to "显示器",
            "ноутбук" to "笔记本电脑", "клавиатура" to "键盘", "мышь" to "鼠标", "принтер" to "打印机",
            "автозапчасть" to "汽车配件", "масло" to "机油", "шина" to "轮胎", "велосипед" to "自行车",
            "самокат" to "滑板车", "рыбалка" to "钓鱼", "спорт" to "运动", "фитнес" to "健身",
            "косметика" to "化妆品", "маска" to "口罩", "витамины" to "维生素", "чай" to "茶",
            "кофе" to "咖啡", "сладости" to "糖果", "ткань" to "面料", "пряжа" to "毛线",
            "упаковка" to "包装", "бутылка" to "瓶子", "коробка" to "纸箱",
            "опт" to "批发", "оптом" to "批发", "фабрика" to "工厂", "завод" to "工厂",
            "производитель" to "厂家", "поставщик" to "供应商", "новый" to "新", "детская" to "儿童",
            "женская" to "女", "мужская" to "男", "большой" to "大", "маленький" to "小",
        )
    }
}
