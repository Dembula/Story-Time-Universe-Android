package com.storytime.universe.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storytime.universe.ui.auth.SignInScreen
import com.storytime.universe.ui.main.MainScaffold
import com.storytime.universe.ui.profiles.ProfilesScreen
import com.storytime.universe.ui.splash.LaunchSplash
import com.storytime.universe.ui.theme.StColors

@Composable
fun RootScreen(appState: AppState = viewModel()) {
    LaunchedEffect(Unit) { appState.bootstrapIfNeeded() }

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        Crossfade(
            targetState = appState.route,
            animationSpec = tween(450),
            label = "route",
        ) { route ->
            when (route) {
                AppState.Route.LOADING -> LaunchSplash()
                AppState.Route.SIGN_IN -> SignInScreen(appState)
                AppState.Route.PROFILES -> ProfilesScreen(appState)
                AppState.Route.MAIN -> MainScaffold(appState)
            }
        }
    }
}
