package com.leantech.finder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.ui.AppViewModel
import com.leantech.finder.ui.UiState
import com.leantech.finder.ui.components.InfoChip
import com.leantech.finder.ui.components.SectionTitle

@Composable
fun SettingsScreen(state: UiState, viewModel: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 14.dp, top = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::openSearch) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Настройки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Аккаунты площадок")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Приложение не хранит логин и пароль. Вход выполняется на официальной " +
                            "странице площадки, а приложение сохраняет только полученную сессию — " +
                            "после входа поиск идёт уже от вашего имени.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Marketplace.entries.forEach { marketplace ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(marketplace.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = marketplace.loginUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.loginStatus[marketplace] == true) {
                                InfoChip(text = "вход выполнен")
                            } else {
                                OutlinedButton(onClick = { viewModel.openLogin(marketplace) }) {
                                    Text("Войти")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::refreshLoginStatus) { Text("Проверить вход") }
                        OutlinedButton(onClick = viewModel::logout) { Text("Выйти везде") }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Перевод")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.translatorEndpoint,
                        onValueChange = viewModel::setTranslatorEndpoint,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Адрес сервиса перевода") },
                        singleLine = true,
                        supportingText = {
                            Text("Пусто — используется публичный переводчик. Если он недоступен из вашей сети, укажите свой адрес с тем же форматом ответа.")
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Переводить названия товаров", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Название каждой карточки переводится на русский (китайский оригинал остаётся ниже).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.translateTitles, onCheckedChange = viewModel::setTranslateTitles)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle("Поиск")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Сколько товаров брать с каждой площадки: ${state.limitPerMarketplace}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = state.limitPerMarketplace.toFloat(),
                        onValueChange = { viewModel.setLimit(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 8,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.proxyBaseUrl,
                        onValueChange = viewModel::setProxyBaseUrl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Адрес своего сервера подписи (необязательно)") },
                        singleLine = true,
                        supportingText = {
                            Text("Площадки подписывают запросы одноразовым токеном. Если прямой запрос не проходит, укажите адрес своего сервера — он подпишет запрос за приложение.")
                        },
                    )
                }
            }

            Text(
                text = "Версия 1.0 · поиск по Taobao, 1688 и 闲鱼 с переводом и описанием продавца",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
