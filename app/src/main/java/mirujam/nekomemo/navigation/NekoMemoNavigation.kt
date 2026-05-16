package mirujam.nekomemo.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mirujam.nekomemo.ui.MainScreen
import mirujam.nekomemo.ui.detail.BankDetailScreen
import mirujam.nekomemo.ui.extract.ExtractScreen
import mirujam.nekomemo.ui.fetcher.FetcherScreen
import mirujam.nekomemo.ui.test.TestScreen

@Composable
fun NekoMemoNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Main.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(250)
            ) + fadeIn(animationSpec = tween(250))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(250)
            ) + fadeIn(animationSpec = tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(250))
        }
    ) {
        composable(Route.Main.route) {
            MainScreen(
                onNavigateToDetail = { bankId ->
                    navController.navigate(Route.Detail.createRoute(bankId))
                },
                onNavigateToFetcher = {
                    navController.navigate(Route.Fetcher.route)
                }
            )
        }

        composable(Route.Fetcher.route) {
            FetcherScreen(navController)
        }

        composable(Route.Extract.route) {
            ExtractScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Detail.route,
            arguments = listOf(
                navArgument("bankId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bankId = backStackEntry.arguments?.getLong("bankId") ?: return@composable
            BankDetailScreen(
                onStartTest = { id, count, shuffleQuestions, shuffleOptions ->
                    navController.navigate(Route.Test.createRoute(id, count, shuffleQuestions, shuffleOptions))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Test.route,
            arguments = listOf(
                navArgument("bankId") { type = NavType.LongType },
                navArgument("questionCount") { type = NavType.IntType },
                navArgument("shuffleQuestions") { type = NavType.BoolType; defaultValue = false },
                navArgument("shuffleOptions") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val bankId = backStackEntry.arguments?.getLong("bankId") ?: return@composable
            val questionCount = backStackEntry.arguments?.getInt("questionCount") ?: return@composable
            val shuffleQuestions = backStackEntry.arguments?.getBoolean("shuffleQuestions") ?: false
            val shuffleOptions = backStackEntry.arguments?.getBoolean("shuffleOptions") ?: false
            TestScreen(
                bankId = bankId,
                questionCount = questionCount,
                shuffleQuestions = shuffleQuestions,
                shuffleOptions = shuffleOptions,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
