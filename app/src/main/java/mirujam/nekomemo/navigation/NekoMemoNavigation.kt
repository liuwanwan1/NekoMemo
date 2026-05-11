package mirujam.nekomemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mirujam.nekomemo.ui.detail.BankDetailScreen
import mirujam.nekomemo.ui.fetcher.FetcherScreen
import mirujam.nekomemo.ui.library.LibraryScreen
import mirujam.nekomemo.ui.settings.SettingsScreen
import mirujam.nekomemo.ui.test.TestScreen

@Composable
fun NekoMemoNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Library.route,
        modifier = modifier
    ) {
        composable(Route.Library.route) {
            LibraryScreen(
                onBankClick = { bankId ->
                    navController.navigate(Route.Detail.createRoute(bankId))
                }
            )
        }

        composable(Route.Fetcher.route) {
            FetcherScreen()
        }

        composable(Route.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Route.Detail.route,
            arguments = listOf(
                navArgument("bankId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bankId = backStackEntry.arguments?.getLong("bankId") ?: return@composable
            BankDetailScreen(
                onStartTest = { id, count ->
                    navController.navigate(Route.Test.createRoute(id, count))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Test.route,
            arguments = listOf(
                navArgument("bankId") { type = NavType.LongType },
                navArgument("questionCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bankId = backStackEntry.arguments?.getLong("bankId") ?: return@composable
            val questionCount = backStackEntry.arguments?.getInt("questionCount") ?: return@composable
            TestScreen(
                bankId = bankId,
                questionCount = questionCount,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
