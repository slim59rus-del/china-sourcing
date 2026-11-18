package com.leantech.finder.di

import android.content.Context
import com.leantech.finder.data.net.CookieStore
import com.leantech.finder.data.net.MtopClient
import com.leantech.finder.data.prefs.SettingsStore
import com.leantech.finder.data.repository.SearchRepository
import com.leantech.finder.data.session.LoginManager
import com.leantech.finder.data.source.Alibaba1688Source
import com.leantech.finder.data.source.MarketplaceSource
import com.leantech.finder.data.source.TaobaoSource
import com.leantech.finder.data.source.XianyuSource
import com.leantech.finder.data.translate.GlossaryTranslator
import com.leantech.finder.data.translate.TranslatorChain
import com.leantech.finder.data.translate.WebTranslator
import com.leantech.finder.domain.seller.SellerSummarizer

/** Простой контейнер зависимостей: один экземпляр на процесс. */
object ServiceLocator {

    lateinit var appContext: Context
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var cookies: CookieStore
        private set
    lateinit var login: LoginManager
        private set
    lateinit var repository: SearchRepository
        private set

    fun init(context: Context) {
        if (::settings.isInitialized) return

        appContext = context.applicationContext
        settings = SettingsStore(appContext)
        cookies = CookieStore(settings)
        login = LoginManager(cookies, settings)

        val mtop = MtopClient(cookies)
        val sources: List<MarketplaceSource> = listOf(
            TaobaoSource(mtop, settings),
            Alibaba1688Source(mtop, settings, cookies, appContext),
            XianyuSource(mtop, settings, appContext),
        )
        val translator = TranslatorChain(
            WebTranslator(settings),
            GlossaryTranslator(),
        )
        repository = SearchRepository(
            sources = sources,
            translator = translator,
            summarizer = SellerSummarizer(translator),
            settings = settings,
        )
    }
}
