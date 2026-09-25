package com.storytime.universe.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.account.AccountScreen
import com.storytime.universe.ui.detail.ContentDetailScreen
import com.storytime.universe.ui.detail.PersonDetailScreen
import com.storytime.universe.ui.downloads.DownloadsScreen
import com.storytime.universe.ui.home.HomeScreen
import com.storytime.universe.ui.mylist.MyListScreen
import com.storytime.universe.ui.player.PlayerScreen
import com.storytime.universe.ui.search.SearchScreen
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("search", "Search", Icons.Filled.Search),
    Tab("downloads", "Downloads", Icons.Filled.Download),
    Tab("mylist", "My List", Icons.AutoMirrored.Filled.List),
    Tab("account", "Account", Icons.Filled.AccountCircle),
)

@Composable
fun MainScaffold(appState: AppState) {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val actions = NavActions(nav, vm)

    // After a successful PPV unlock, auto-start the pending playback.
    LaunchedEffect(appState.paywallContext) {
        if (appState.paywallContext == null) {
            delay(350)
            val resume = appState.consumeResumePlaybackAfterUnlock()
            if (resume != null) actions.play(resume)
        }
    }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = StColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color(0xFF0A0A0A)) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StColors.Accent,
                                selectedTextColor = StColors.Accent,
                                unselectedIconColor = StColors.NavInactive,
                                unselectedTextColor = StColors.NavInactive,
                                indicatorColor = Color(0xFF1A1207),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") { HomeScreen(appState, actions) }
            composable("search") { SearchScreen(actions) }
            composable("downloads") { DownloadsScreen(actions) }
            composable("mylist") { MyListScreen(actions) }
            composable("account") { AccountScreen(appState) }

            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ContentDetailScreen(
                    contentId = id,
                    seed = vm.seeds[id],
                    actions = actions,
                    appState = appState,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("person/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val route = vm.personRoutes[id]
                PersonDetailScreen(
                    route = route,
                    actions = actions,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("player") {
                PlayerScreen(
                    request = vm.pendingPlayback,
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }
}
