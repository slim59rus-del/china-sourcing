package com.leantech.finder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.ui.AppViewModel
import com.leantech.finder.ui.UiState
import com.leantech.finder.ui.components.InfoChip
import com.leantech.finder.ui.components.ProductCard
import com.leantech.finder.ui.components.SectionTitle
import com.leantech.finder.ui.components.openLink
import com.leantech.finder.ui.theme.Navy
import com.leantech.finder.ui.theme.NavyDeep
import com.leantech.finder.ui.theme.Coral

@Composable
fun SearchScreen(state: UiState, viewModel: AppViewModel) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Header(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::search,
            onSettings = viewModel::openSettings,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Filters(state, viewModel)

            if (state.loading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(state.stage.ifBlank { "Ищу..." }, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            state.translatedQuery?.takeIf { state.hasSearched }?.let { chinese ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoChip(
                        text = "запрос на китайском: $chinese",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "за ${state.tookMs} мс",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.problems.forEach { problem ->
                ProblemCard(
                    marketplaceTitle = problem.marketplace.title,
                    message = problem.message,
                    onOpenWeb = { openLink(context, problem.webFallbackUrl) },
                    onLogin = {
                        openLink(context, problem.marketplace.loginUrl)
                    },
                )
            }

            if (state.products.isNotEmpty()) {
                SectionTitle("Найдено товаров: ${state.products.size}")
            }
            state.products.forEachIndexed { index, product ->
                ProductCard(product = product, index = index)
            }

            if (!state.hasSearched && !state.loading) {
                Hint()
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Header(
    state: UiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val gradient = Brush.verticalGradient(listOf(NavyDeep, Navy))
    Box(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 8.dp, top = 28.dp, bottom = 18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Поиск по Китаю",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Taobao · 1688 · 闲鱼 — с переводом и описанием продавца",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Что искать? Например: складной стул", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSearch,
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.height(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.loading) "Ищу..." else "Искать на ${state.selectedSources.size} площадках")
                }
            }
        }
    }
}

@Composable
private fun Filters(state: UiState, viewModel: AppViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            SectionTitle("Где искать")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Marketplace.entries.forEach { marketplace ->
                    FilterChip(
                        selected = marketplace in state.selectedSources,
                        onClick = { viewModel.toggleSource(marketplace) },
                        label = { Text(marketplace.title) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("Сортировка")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sort == SortOption.SALES_DESC,
                        onClick = { viewModel.setSort(SortOption.SALES_DESC) },
                        label = { Text("По продажам") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.height(16.dp)) },
                    )
                    FilterChip(
                        selected = state.sort == SortOption.PRICE_ASC,
                        onClick = { viewModel.setSort(SortOption.PRICE_ASC) },
                        label = { Text("Сначала дешёвые") },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sort == SortOption.PRICE_DESC,
                        onClick = { viewModel.setSort(SortOption.PRICE_DESC) },
                        label = { Text("Сначала дорогие") },
                    )
                    FilterChip(
                        selected = state.sort == SortOption.RELEVANCE,
                        onClick = { viewModel.setSort(SortOption.RELEVANCE) },
                        label = { Text("Как на площадке") },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("Фильтр «фабрика»")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Только фабрики и производители", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Оставляем продавцов с признаками производства " +
                            "(厂家, 工厂, 源头工厂, 实力商家 и другие метки площадки).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.factoryOnly, onCheckedChange = viewModel::setFactoryOnly)
            }
        }
    }
}

@Composable
private fun ProblemCard(
    marketplaceTitle: String,
    message: String,
    onOpenWeb: () -> Unit,
    onLogin: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.height(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$marketplaceTitle: нужны данные аккаунта или другой запрос",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLogin) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Войти в аккаунт")
                }
                TextButton(onClick = onOpenWeb) { Text("Открыть поиск на сайте") }
            }
        }
    }
}

@Composable
private fun Hint() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Как это работает", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Запрос уходит на китайский автоматически — писать по-китайски не нужно.",
                "Отметьте одну, две или все три площадки: Taobao, 1688, 闲鱼.",
                "«По продажам» ставит наверх товары с наибольшим числом продаж.",
                "«Только фабрики» оставляет продавцов с признаками производства.",
                "Каждый результат — ссылка на товар и на страницу продавца плюс краткое описание продавца по-русски.",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = Coral, modifier = Modifier.width(14.dp))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
