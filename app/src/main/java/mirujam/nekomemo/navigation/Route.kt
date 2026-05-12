package mirujam.nekomemo.navigation

sealed class Route(val route: String, val title: String) {
    data object Library : Route("library", "Library")
    data object Fetcher : Route("fetcher", "Fetcher")
    data object Settings : Route("settings", "Settings")
    data object Detail : Route("detail/{bankId}", "Detail") {
        fun createRoute(bankId: Long): String = "detail/$bankId"
    }
    data object Test : Route("test/{bankId}/{questionCount}", "Test") {
        fun createRoute(bankId: Long, questionCount: Int): String = "test/$bankId/$questionCount"
    }
    data object Extract : Route("extract?jsonData={jsonData}", "Extract") {
        fun createRoute(jsonData: String): String = "extract?jsonData=${java.net.URLEncoder.encode(jsonData, "UTF-8")}"
        const val BASE_ROUTE = "extract"
    }
}
