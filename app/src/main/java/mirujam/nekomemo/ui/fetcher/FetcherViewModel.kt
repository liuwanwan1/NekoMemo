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
import kotlinx.coroutines.withTimeoutOrNull
import mirujam.nekomemo.domain.usecase.HtmlParserUseCase
import mirujam.nekomemo.domain.usecase.decodeHtmlFromJs
import mirujam.nekomemo.ui.model.FetcherUiState
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class FetcherViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "FetcherViewModel"
        private const val PARSE_TIMEOUT_MS = 30_000L
    }

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
        if (html.length > 500_000) {
            Log.w(TAG, "Large HTML detected (${html.length} chars), using optimized parsing")
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, parseResult = null)
            
            try {
                val result = withTimeoutOrNull(PARSE_TIMEOUT_MS) {
                    withContext(Dispatchers.Default) {
                        HtmlParserUseCase.parse(html)
                    }
                } ?: run {
                    Log.e(TAG, "Parsing timed out after ${PARSE_TIMEOUT_MS}ms")
                    _uiState.value = _uiState.value.copy(
                        parseResult = "Parsing timeout - try a smaller page",
                        isParsing = false
                    )
                    return@launch
                }

                if (result.questions.isEmpty()) {
                    Log.w(TAG, "No questions found in parsed result!")
                    _uiState.value = _uiState.value.copy(
                        parseResult = "No questions found on this page",
                        isParsing = false
                    )
                } else {
                    val jsonString = result.toJson()
                    Log.d(TAG, "Parse success! Questions: ${result.questions.size}, JSON size: ${jsonString.length}")
                    
                    _uiState.value = _uiState.value.copy(
                        extractedJson = jsonString,
                        navigateToExtract = true,
                        isParsing = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing HTML", e)
                _uiState.value = _uiState.value.copy(
                    parseResult = "Error: ${e.message}",
                    isParsing = false
                )
            }
        }
    }

    fun getExtractedJson(): String? {
        val json = _uiState.value.extractedJson
        Log.d(TAG, "getExtractedJson() called, returning JSON with length: ${json?.length ?: 0}")
        return json
    }

    fun onNavigatedToExtract() {
        Log.d(TAG, "onNavigatedToExtract() called")
        _uiState.value = _uiState.value.copy(navigateToExtract = false)
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(parseResult = null)
    }

    fun releaseMemory() {
        Log.d(TAG, "releaseMemory() called - clearing extractedJson and large objects")
        _uiState.value = _uiState.value.copy(
            extractedJson = null,
            urlInput = ""
        )
    }

    fun decodeHtml(raw: String?): String = decodeHtmlFromJs(raw)
}
