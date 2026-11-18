package com.leantech.finder.data.net

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Выполняет загрузку страницы поиска в скрытом WebView с авторизованными куками CookieManager
 * и извлекает данные карточек товаров (window.__INIT_DATA или DOM-элементы), обходя RGV587_ERROR.
 */
object WebViewSearchFetcher {

    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchJsonOrDom(
        context: Context,
        searchUrl: String,
        timeoutMs: Long = 16_000L,
    ): JSONObject? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            mainHandler.post {
                val appCtx = context.applicationContext
                val webView = WebView(appCtx)
                var finished = false

                fun cleanupAndResume(result: JSONObject?) {
                    if (finished) return
                    finished = true
                    runCatching {
                        webView.stopLoading()
                        webView.destroy()
                    }
                    if (cont.isActive) {
                        cont.resume(result)
                    }
                }

                cont.invokeOnCancellation {
                    mainHandler.post { cleanupAndResume(null) }
                }

                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.databaseEnabled = true
                webView.settings.loadsImagesAutomatically = false
                webView.settings.blockNetworkImage = true
                webView.settings.userAgentString = MtopClient.MOBILE_UA

                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(webView, true)

                val pollJs = """
                    (function() {
                        try {
                            if (window.__INIT_DATA && typeof window.__INIT_DATA === 'object') {
                                var raw = JSON.stringify(window.__INIT_DATA);
                                if (raw && raw.length > 500) {
                                    return JSON.stringify({ source: 'init_data', payload: window.__INIT_DATA });
                                }
                            }
                            var items = [];
                            var anchors = document.querySelectorAll('a[href*="offer/"], a[href*="item.htm"], a[href*="item?id="], a[href*="://goofish.com"]');
                            for (var i = 0; i < anchors.length; i++) {
                                var a = anchors[i];
                                var href = a.href || '';
                                var card = a.closest('[class*="card"], [class*="item"], [class*="offer"], li, div') || a;
                                var text = (card.innerText || '').trim();
                                if (!text || text.length < 4) continue;
                                var lines = text.split('\n').map(function(s){ return s.trim(); }).filter(Boolean);
                                var title = lines[0] || '';
                                var price = '';
                                var sales = '';
                                for (var j = 0; j < lines.length; j++) {
                                    var ln = lines[j];
                                    if (!price && (ln.indexOf('¥') !== -1 || ln.indexOf('￥') !== -1 || /^\d+(\.\d+)?$/.test(ln))) {
                                        price = ln;
                                    }
                                    if (!sales && (ln.indexOf('成交') !== -1 || ln.indexOf('人付款') !== -1 || ln.indexOf('想要') !== -1 || ln.indexOf('已售') !== -1)) {
                                        sales = ln;
                                    }
                                    if (ln.length > title.length && ln.indexOf('¥') === -1 && ln.indexOf('￥') === -1) {
                                        title = ln;
                                    }
                                }
                                var imgEl = card.querySelector('img');
                                var img = imgEl ? (imgEl.getAttribute('src') || imgEl.getAttribute('data-src') || '') : '';
                                if (title && (price || href)) {
                                    items.push({
                                        title: title,
                                        price: price,
                                        view_sales: sales,
                                        url: href,
                                        pic_url: img
                                    });
                                }
                            }
                            if (items.length > 0) {
                                return JSON.stringify({ source: 'dom', items: items });
                            }
                        } catch (e) {}
                        return '';
                    })();
                """.trimIndent()
                var attempts = 0
                val maxAttempts = 18

                val poller = object : Runnable {
                    override fun run() {
                        if (finished) return
                        attempts++
                        webView.evaluateJavascript(pollJs) { rawResult ->
                            if (finished) return@evaluateJavascript
                            val cleaned = unquoteJsString(rawResult)
                            if (cleaned.startsWith("{")) {
                                val parsed = runCatching { JSONObject(cleaned) }.getOrNull()
                                if (parsed != null) {
                                    cm.flush()
                                    val payloadObj = parsed.optJSONObject("payload") ?: parsed
                                    cleanupAndResume(payloadObj)
                                    return@evaluateJavascript
                                }
                            }
                            if (attempts >= maxAttempts) {
                                cleanupAndResume(null)
                            } else {
                                mainHandler.postDelayed(this, 750L)
                            }
                        }
                    }
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val scheme = request?.url?.scheme?.lowercase().orEmpty()
                        return scheme != "http" && scheme != "https"
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        cm.flush()
                        if (!finished && attempts == 0) {
                            mainHandler.postDelayed(poller, 900L)
                        }
                    }
                }

                webView.loadUrl(searchUrl)
            }
        }
    }

    private fun unquoteJsString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        if (raw.length >= 2 && raw.first() == '"' && raw.last() == '"') {
            return runCatching {
                JSONObject("{\"v\":" + raw + "}").getString("v")
            }.getOrDefault("")
        }
        return raw
    }
}
