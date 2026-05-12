package mirujam.nekomemo.ui.fetcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mirujam.nekomemo.domain.usecase.HtmlParserUseCase
import mirujam.nekomemo.domain.usecase.decodeHtmlFromJs
import mirujam.nekomemo.ui.model.FetcherUiState
import javax.inject.Inject

@HiltViewModel
class FetcherViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FetcherUiState())
    val uiState: StateFlow<FetcherUiState> = _uiState.asStateFlow()

    val isParsing: StateFlow<Boolean> = _uiState.mapToField { it.isParsing }
    val parseResult: StateFlow<String?> = _uiState.mapToField { it.parseResult }
    val currentUrl: StateFlow<String> = _uiState.mapToField { it.currentUrl.ifBlank { "https://i.chaoxing.com" } }
    val navigateToExtract: StateFlow<Boolean> = _uiState.mapToField { it.navigateToExtract }

    private fun <T, R> MutableStateFlow<T>.mapToField(mapper: (T) -> R): StateFlow<R> {
        val result = MutableStateFlow(mapper(value))
        viewModelScope.launch {
            collect { result.value = mapper(it) }
        }
        return result.asStateFlow()
    }

    fun setUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url)
    }

    fun setCurrentUrl(url: String) {
        _uiState.value = _uiState.value.copy(currentUrl = url, urlInput = url)
    }

    fun parseHtml(html: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, parseResult = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    HtmlParserUseCase.parse(html)
                }
                if (result.questions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        parseResult = "No questions found on this page",
                        isParsing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        extractedJson = result.toJson(),
                        navigateToExtract = true,
                        isParsing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    parseResult = "Error: ${e.message}",
                    isParsing = false
                )
            }
        }
    }

    fun getExtractedJson(): String? = _uiState.value.extractedJson

    fun onNavigatedToExtract() {
        _uiState.value = _uiState.value.copy(navigateToExtract = false)
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(parseResult = null)
    }

    fun decodeHtml(raw: String?): String = decodeHtmlFromJs(raw)
}
