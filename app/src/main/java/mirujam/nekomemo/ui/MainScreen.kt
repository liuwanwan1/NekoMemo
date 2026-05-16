package mirujam.nekomemo.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mirujam.nekomemo.navigation.BottomNavBar
import mirujam.nekomemo.navigation.Route
import mirujam.nekomemo.ui.library.LibraryScreen
import mirujam.nekomemo.ui.settings.SettingsScreen

@Composable
fun MainScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToFetcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Route.Library.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Library.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(Route.Library.route) {
                LibraryScreen(
                    onBankClick = onNavigateToDetail,
                    onNavigateToFetcher = onNavigateToFetcher
                )
            }
            composable(Route.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
