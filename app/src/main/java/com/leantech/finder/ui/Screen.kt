package com.leantech.finder.ui

import com.leantech.finder.domain.model.Marketplace

/** Экраны приложения. */
sealed interface Screen {
    data object Search : Screen
    data object Settings : Screen
    data class Login(val marketplace: Marketplace) : Screen
}
