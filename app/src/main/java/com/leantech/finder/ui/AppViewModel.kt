package com.leantech.finder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.data.repository.SearchRepository
import com.leantech.finder.data.session.LoginManager
import com.leantech.finder.di.ServiceLocator
import com.leantech.finder.domain.model.Marketplace
import com.leantech.finder.domain.model.Product
import com.leantech.finder.domain.model.SearchRequest
import com.leantech.finder.domain.model.SortOption
import com.leantech.finder.domain.model.SourceProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val query: String = "",
    val selectedSources: Set<Marketplace> = setOf(Marketplace.ALIBABA_1688),
    val factoryOnly: Boolean = false,
    val sort: SortOption = SortOption.SALES_DESC,
    val limitPerMarketplace: Int = 30,
    val translateTitles: Boolean = true,
    val translatorEndpoint: String = "",
    val proxyBaseUrl: String = "",
    val loading: Boolean = false,
    val stage: String = "",
    val translatedQuery: String? = null,
    val products: List<Product> = emptyList(),
    val problems: List<SourceProblem> = emptyList(),
    val loginStatus: Map<Marketplace, Boolean> = emptyMap(),
    val tookMs: Long = 0,
    val hasSearched: Boolean = false,
    val message: String? = null,
    val screen: Screen = Screen.Search,
)

class AppViewModel(
    private val repository: SearchRepository = ServiceLocator.repository,
    private val settings: SettingsStore = ServiceLocator.settings,
    private val login: LoginManager = ServiceLocator.login,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UiState(
            query = settings.lastQuery,
            selectedSources = settings.selectedSources,
            factoryOnly = settings.factoryOnly,
            sort = settings.sort,
            limitPerMarketplace = settings.resultLimit,
            translateTitles = settings.translateTitles,
            translatorEndpoint = settings.translatorEndpoint,
            proxyBaseUrl = settings.proxyBaseUrl,
            loginStatus = login.refresh(),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun toggleSource(marketplace: Marketplace) = _state.update { current ->
        val next = current.selectedSources.toMutableSet()
        if (!next.add(marketplace)) next.remove(marketplace)
        if (next.isEmpty()) next.add(marketplace) // хотя бы одна площадка всегда выбрана
        settings.selectedSources = next
        current.copy(selectedSources = next)
    }

    fun setFactoryOnly(value: Boolean) {
        settings.factoryOnly = value
        _state.update { it.copy(factoryOnly = value) }
    }

    fun setSort(value: SortOption) {
        settings.sort = value
        _state.update { it.copy(sort = value) }
    }

    fun setLimit(value: Int) {
        settings.resultLimit = value
        _state.update { it.copy(limitPerMarketplace = settings.resultLimit) }
    }

    fun setTranslateTitles(value: Boolean) {
        settings.translateTitles = value
        _state.update { it.copy(translateTitles = value) }
    }

    fun setTranslatorEndpoint(value: String) {
        settings.translatorEndpoint = value
        _state.update { it.copy(translatorEndpoint = value) }
    }

    fun setProxyBaseUrl(value: String) {
        settings.proxyBaseUrl = value
        _state.update { it.copy(proxyBaseUrl = value) }
    }

    fun openSettings() = _state.update { it.copy(screen = Screen.Settings) }

    fun openSearch() = _state.update { it.copy(screen = Screen.Search) }

    fun openLogin(marketplace: Marketplace) = _state.update { it.copy(screen = Screen.Login(marketplace)) }

    fun closeLogin() = _state.update {
        it.copy(screen = Screen.Search, loginStatus = login.refresh())
    }

    fun refreshLoginStatus() = _state.update { it.copy(loginStatus = login.refresh()) }

    fun logout() {
        login.logout()
        _state.update {
            it.copy(
                loginStatus = login.refresh(),
                message = "Сессии аккаунтов удалены",
            )
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun search() {
        val current = _state.value
        if (current.query.isBlank()) {
            _state.update { it.copy(message = "Введите, что искать") }
            return
        }
        if (current.loading) return

        settings.lastQuery = current.query
        val request = SearchRequest(
            query = current.query.trim(),
            marketplaces = current.selectedSources,
            factoryOnly = current.factoryOnly,
            sort = current.sort,
            limitPerMarketplace = current.limitPerMarketplace,
        )

        viewModelScope.launch {
            _state.update { it.copy(loading = true, stage = "Готовлю запрос", problems = emptyList()) }
            val outcome = runCatching {
                repository.search(request) { stage -> _state.update { it.copy(stage = stage) } }
            }.getOrElse { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        message = error.message ?: "Поиск не удался",
                        hasSearched = true,
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    loading = false,
                    stage = "",
                    translatedQuery = outcome.translatedQuery,
                    products = outcome.products,
                    problems = outcome.problems,
                    tookMs = outcome.tookMs,
                    hasSearched = true,
                    message = if (outcome.products.isEmpty() && outcome.problems.isEmpty()) {
                        "Ничего не найдено. Попробуйте другие слова или включите другую площадку."
                    } else null,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel() as T
        }
    }
}
