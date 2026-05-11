package mirujam.nekomemo.navigation

sealed class Route(val route: String) {
    data object Library : Route("library")
    data object Fetcher : Route("fetcher")
    data object Settings : Route("settings")
    data object Detail : Route("detail/{bankId}") {
        fun createRoute(bankId: Long): String = "detail/$bankId"
    }
    data object Test : Route("test/{bankId}/{questionCount}") {
        fun createRoute(bankId: Long, questionCount: Int): String = "test/$bankId/$questionCount"
    }
}
