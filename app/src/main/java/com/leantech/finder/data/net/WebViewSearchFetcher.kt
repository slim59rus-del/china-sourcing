package com.leantech.finder.data.net

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Загружает страницу поиска в виртуальном WebView (с заданными размерами экрана для движка Chromium),
 * перехватывает XHR/fetch ответы и извлекает JSON или DOM-карточки товаров.
 */
object WebViewSearchFetcher {

    private val mainHandler = Handler(Looper.getMainLooper())

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchJsonOrDom(
        context: Context,
        searchUrl: String,
        timeoutMs: Long = 22_000L,
    ): JSONObject? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            mainHandler.post {
                val appCtx = context.applicationContext
                val webView = WebView(appCtx)
                var finished = false
                var lastLoadedUrl = ""
                var lastPageTitle = ""
                var lastErrorText = ""

                fun cleanupAndResume(result: JSONObject?) {
                    if (finished) return
                    finished = true
                    runCatching {
                        webView.stopLoading()
                        webView.onPause()
                        webView.destroy()
                    }
                    if (cont.isActive) {
                        cont.resume(result)
                    }
                }

                cont.invokeOnCancellation {
                    mainHandler.post { cleanupAndResume(null) }
                }

                // Задаём виртуальные размеры экрана, иначе Chromium на EMUI/Android не рендерит DOM и зависает на about:blank
                val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                webView.measure(widthSpec, heightSpec)
                webView.layout(0, 0, 1080, 1920)
                webView.onResume()

                val isMobileSite = searchUrl.contains("://1688.com") || searchUrl.contains("://1688.com")
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.databaseEnabled = true
                webView.settings.loadsImagesAutomatically = false
                webView.settings.blockNetworkImage = true
                webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webView.settings.setSupportMultipleWindows(false)
                webView.settings.javaScriptCanOpenWindowsAutomatically = false
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
                webView.settings.userAgentString = if (isMobileSite) MtopClient.MOBILE_UA else DESKTOP_UA

                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(webView, true)

                val hookScript = """
                    (function() {
                        if (window.__finderHooked) return;
                        window.__finderHooked = true;
                        window.__capturedApiJson = null;

                        function checkPayload(text) {
                            if (!text || text.length < 300) return;
                            var clean = text.trim();
                            if (clean.indexOf('(') > 0 && clean.indexOf('(') < 50 && clean.endsWith(')')) {
                                clean = clean.substring(clean.indexOf('(') + 1, clean.length - 1);
                            }
                            if (clean.startsWith('{') && (
                                clean.indexOf('offerList') !== -1 ||
                                clean.indexOf('resultList') !== -1 ||
                                clean.indexOf('itemId') !== -1 ||
                                clean.indexOf('offerId') !== -1 ||
                                clean.indexOf('simpleSubject') !== -1 ||
                                clean.indexOf('picUrl') !== -1 ||
                                clean.indexOf('subject') !== -1
                            )) {
                                window.__capturedApiJson = clean;
                            }
                        }

                        var origFetch = window.fetch;
                        if (origFetch) {
                            window.fetch = function() {
                                return origFetch.apply(this, arguments).then(function(resp) {
                                    try {
                                        resp.clone().text().then(checkPayload);
                                    } catch (e) {}
                                    return resp;
                                });
                            };
                        }

                        var origOpen = XMLHttpRequest.prototype.open;
                        var origSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.open = function(method, url) {
                            this.__reqUrl = url || '';
                            return origOpen.apply(this, arguments);
                        };
                        XMLHttpRequest.prototype.send = function() {
                            this.addEventListener('load', function() {
                                try {
                                    checkPayload(this.responseText);
                                } catch (e) {}
                            });
                            return origSend.apply(this, arguments);
                        };
                    })();
                """.trimIndent()
                val pollJs = """
                    (function() {
                        try {
                            if (window.__capturedApiJson) {
                                return window.__capturedApiJson;
                            }
                            if (window.__INIT_DATA && typeof window.__INIT_DATA === 'object') {
                                var raw = JSON.stringify(window.__INIT_DATA);
                                if (raw && raw.length > 500 && (
                                    raw.indexOf('offerId') !== -1 ||
                                    raw.indexOf('simpleSubject') !== -1 ||
                                    raw.indexOf('subject') !== -1
                                )) {
                                    return raw;
                                }
                            }
                            if (window.data && typeof window.data === 'object') {
                                var rawData = JSON.stringify(window.data);
                                if (rawData && rawData.length > 500 && (
                                    rawData.indexOf('offerId') !== -1 ||
                                    rawData.indexOf('subject') !== -1
                                )) {
                                    return rawData;
                                }
                            }
                            var items = [];
                            var anchors = document.querySelectorAll(
                                'a[href*="offer/"], a[href*="offerId="], a[href*="://goofish.com"], a[href*="item.htm"]'
                            );
                            for (var i = 0; i < anchors.length; i++) {
                                var a = anchors[i];
                                var href = a.href || '';
                                if (href.indexOf('offer') === -1 && href.indexOf('item') === -1) continue;
                                var card = a.closest('[class*="offer"], [class*="card"], [class*="item"], [class*="feeds"], [data-offer-id], li') || a.parentElement || a;
                                var text = (card.innerText || '').trim();
                                if (!text || text.length < 4) continue;
                                var lines = text.split('\n').map(function(s){ return s.trim(); }).filter(Boolean);
                                var title = '';
                                var price = '';
                                var sales = '';
                                for (var j = 0; j < lines.length; j++) {
                                    var ln = lines[j];
                                    if (!price && (ln.indexOf('¥') !== -1 || ln.indexOf('￥') !== -1 || /^\d+(\.\d+)?$/.test(ln))) {
                                        price = ln;
                                    }
                                    if (!sales && (ln.indexOf('成交') !== -1 || ln.indexOf('人付款') !== -1 || ln.indexOf('想要') !== -1 || ln.indexOf('已售') !== -1 || ln.indexOf('件') !== -1)) {
                                        sales = ln;
                                    }
                                    if (ln.length > title.length && ln.indexOf('¥') === -1 && ln.indexOf('￥') === -1 && ln.length > 4) {
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
                                return JSON.stringify({ items: items });
                            }
                        } catch (e) {}
                        var bodySnippet = (document.body && document.body.innerText) ? document.body.innerText.replace(/\s+/g, ' ').trim().slice(0, 120) : '';
                        return JSON.stringify({
                            __diagUrl: location.href || '',
                            __diagTitle: document.title || '',
                            __diagText: bodySnippet
                        });
                    })();
                """.trimIndent()

                var attempts = 0
                val maxAttempts = 22
                var pollerStarted = false

                val poller = object : Runnable {
                    override fun run() {
                        if (finished) return
                        attempts++
                        webView.evaluateJavascript(hookScript, null)
                        webView.evaluateJavascript(pollJs) { rawResult ->
                            if (finished) return@evaluateJavascript
                            val cleaned = unquoteJsString(rawResult)
                            if (cleaned.startsWith("{")) {
                                val parsed = runCatching { JSONObject(cleaned) }.getOrNull()
                                if (parsed != null) {
                                    if (parsed.has("__diagUrl")) {
                                        val u = parsed.optString("__diagUrl")
                                        if (u.isNotBlank() && u != "about:blank") {
                                            lastLoadedUrl = u
                                        }
                                        val t = parsed.optString("__diagTitle")
                                        if (t.isNotBlank()) lastPageTitle = t
                                        val txt = parsed.optString("__diagText")
                                        if (txt.isNotBlank()) lastErrorText = txt
                                    } else {
                                        cm.flush()
                                        cleanupAndResume(parsed)
                                        return@evaluateJavascript
                                    }
                                }
                            }
                            if (attempts >= maxAttempts) {
                                val diag = JSONObject().apply {
                                    put("__debugUrl", lastLoadedUrl.ifBlank { searchUrl })
                                    put("__debugTitle", lastPageTitle)
                                    put("__debugText", lastErrorText)
                                }
                                cleanupAndResume(diag)
                            } else {
                                mainHandler.postDelayed(this, 800L)
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

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrBlank() && url != "about:blank") {
                            lastLoadedUrl = url
                        }
                        view?.evaluateJavascript(hookScript, null)
                        if (!pollerStarted && !finished) {
                            pollerStarted = true
                            mainHandler.postDelayed(poller, 1200L)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!url.isNullOrBlank() && url != "about:blank") {
                            lastLoadedUrl = url
                        }
                        view?.evaluateJavascript(hookScript, null)
                        cm.flush()
                        if (!pollerStarted && !finished) {
                            pollerStarted = true
                            mainHandler.postDelayed(poller, 800L)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            lastErrorText = "HTTP/Net error: " + (error?.description?.toString().orEmpty())
                        }
                    }
                }

                // Запускаем опрос сразу на случай, если onPageStarted/Finished задержатся
                pollerStarted = true
                mainHandler.postDelayed(poller, 1500L)
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
