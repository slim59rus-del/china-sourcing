package com.leantech.finder

import android.app.Application
import com.leantech.finder.di.ServiceLocator

class FinderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
