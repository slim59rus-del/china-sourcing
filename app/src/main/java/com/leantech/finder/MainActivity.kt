package com.leantech.finder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leantech.finder.ui.AppViewModel
import com.leantech.finder.ui.Screen
import com.leantech.finder.ui.screens.SearchScreen
import com.leantech.finder.ui.screens.SettingsScreen
import com.leantech.finder.ui.screens.WebLoginScreen
import com.leantech.finder.ui.theme.ChinaSourcingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChinaSourcingTheme {
                val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
                val state by viewModel.state.collectAsStateWithLifecycle()

                when (val screen = state.screen) {
                    Screen.Search -> SearchScreen(state = state, viewModel = viewModel)
                    Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
                    is Screen.Login -> WebLoginScreen(
                        marketplace = screen.marketplace,
                        onClosed = viewModel::closeLogin,
                        onLoggedIn = viewModel::refreshLoginStatus,
                    )
                }
            }
        }
    }
}
