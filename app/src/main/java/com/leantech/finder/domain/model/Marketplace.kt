package com.leantech.finder.domain.model

/**
 * Площадка, по которой идёт поиск.
 *
 * searchUrlTemplate — веб-страница поиска (открывается в браузере, когда запрос по API не прошёл).
 * loginUrl — страница входа, которую открывает встроенный браузер приложения.
 */
enum class Marketplace(
    val key: String,
    val title: String,
    val chineseName: String,
    val searchUrlTemplate: String,
    val loginUrl: String,
    val apiHost: String,
    val needsLogin: Boolean,
    val accentHex: String,
) {
    TAOBAO(
        key = "taobao",
        title = "Taobao",
        chineseName = "淘宝",
        searchUrlTemplate = "https://s.taobao.com/search?q=%s",
        loginUrl = "https://login.m.taobao.com/msg_login.htm",
        apiHost = "https://h5api.m.taobao.com",
        needsLogin = true,
        accentHex = "FFE07A5F",
    ),
    ALIBABA_1688(
        key = "1688",
        title = "1688",
        chineseName = "阿里巴巴",
        searchUrlTemplate = "https://s.1688.com/selloffer/offer_search.htm?keywords=%s",
        loginUrl = "https://login.1688.com/member/signin.htm",
        apiHost = "https://h5api.m.1688.com",
        needsLogin = true,
        accentHex = "FF3D8361",
    ),
    XIANYU(
        key = "xianyu",
        title = "闲鱼 Idle Fish",
        chineseName = "闲鱼",
        searchUrlTemplate = "https://www.goofish.com/search?q=%s",
        loginUrl = "https://passport.goofish.com/",
        apiHost = "https://h5api.m.goofish.com",
        needsLogin = true,
        accentHex = "FFF2C14E",
    );
}
