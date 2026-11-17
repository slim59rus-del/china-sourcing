package com.leantech.finder.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.leantech.finder.di.ServiceLocator
import com.leantech.finder.domain.model.Marketplace

/**
 * Вход в аккаунт площадки. Открывается официальная страница входа в встроенном браузере:
 * пароль вводится только на площадке и приложению не передаётся. Полученная сессия остаётся
 * в общем хранилище cookie, поэтому дальше поиск идёт от имени пользователя.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginScreen(
    marketplace: Marketplace,
    onClosed: () -> Unit,
    onLoggedIn: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Вход: ${marketplace.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Введите логин и пароль на странице площадки. Пароль вводится только здесь.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClosed) { Text("Готово") }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            CookieManager.getInstance().flush()
                            if (ServiceLocator.login.isLoggedIn(marketplace)) {
                                onLoggedIn()
                            }
                        }
                    }
                    loadUrl(marketplace.loginUrl)
                }
            },
        )
    }
}
