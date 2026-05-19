package mirujam.nekomemo.ui.fetcher

import mirujam.nekomemo.ui.model.UiText

data class FetcherUiState(
    val isParsing: Boolean = false,
    val parseResult: UiText? = null,
    val currentUrl: String = "https://i.chaoxing.com",
    val urlInput: String = "https://i.chaoxing.com",
    val navigateToExtract: Boolean = false,
    val extractedJson: String? = null
)
