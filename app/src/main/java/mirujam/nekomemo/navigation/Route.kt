package mirujam.nekomemo.navigation

sealed class Route(val route: String, val title: String) {
    data object Library : Route("library", "Library")
    data object Fetcher : Route("fetcher", "Fetcher")
    data object Settings : Route("settings", "Settings")
    data object Extract : Route("extract", "Extract")
    data object Detail : Route("detail?bankId={bankId}", "Detail") {
        fun createRoute(bankId: Long): String = "detail?bankId=$bankId"
    }
    data object Test : Route("test?bankId={bankId}&questionCount={questionCount}", "Test") {
        fun createRoute(bankId: Long, questionCount: Int): String = "test?bankId=$bankId&questionCount=$questionCount"
    }
}
