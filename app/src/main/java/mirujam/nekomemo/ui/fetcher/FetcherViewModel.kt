package mirujam.nekomemo.ui.fetcher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mirujam.nekomemo.R
import mirujam.nekomemo.domain.usecase.HtmlParserUseCase
import mirujam.nekomemo.ui.model.FetcherUiState
import mirujam.nekomemo.ui.model.UiText
import mirujam.nekomemo.data.model.ExtractedQuestionBankSerializer
import mirujam.nekomemo.ui.shared.SharedDataStore
import javax.inject.Inject

@HiltViewModel
class FetcherViewModel @Inject constructor(
    private val sharedDataStore: SharedDataStore,
    private val htmlParserUseCase: HtmlParserUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "FetcherViewModel"
        private const val PARSE_TIMEOUT_MS = 30_000L
        
        const val MAX_HTML_SIZE = 2 * 1024 * 1024
        const val MAX_RESULT_JSON_SIZE = 5 * 1024 * 1024
        const val WARNING_HTML_SIZE = 500_000
    }

    private val _uiState = MutableStateFlow(FetcherUiState())

    val uiState: StateFlow<FetcherUiState> = _uiState.asStateFlow()

    fun setCurrentUrl(url: String) {
        _uiState.value = _uiState.value.copy(currentUrl = url, urlInput = url)
    }

    fun parseHtml(html: String) {
        if (html.isBlank()) {
            Log.w(TAG, "parseHtml: HTML is blank")
            _uiState.value = _uiState.value.copy(
                parseResult = UiText.StringResource(R.string.fetcher_error_empty_html),
                isParsing = false
            )
            return
        }
        
        val htmlSize = html.length
        when {
            htmlSize > MAX_HTML_SIZE -> {
                val sizeInMB = htmlSize / 1024.0 / 1024.0
                Log.e(TAG, "parseHtml: HTML too large ($htmlSize chars > ${MAX_HTML_SIZE / 1024 / 1024}MB), rejecting")
                _uiState.value = _uiState.value.copy(
                    parseResult = UiText.StringResource(R.string.fetcher_error_page_too_large, arrayOf(sizeInMB)),
                    isParsing = false
                )
                return
            }
            htmlSize > WARNING_HTML_SIZE -> {
                Log.w(TAG, "parseHtml: Large HTML detected ($htmlSize chars), will optimize")
            }
            else -> {
                Log.d(TAG, "parseHtml: Parsing HTML of size $htmlSize chars")
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, parseResult = null)
            
            try {
                val safeHtml = if (htmlSize > WARNING_HTML_SIZE) {
                    Log.d(TAG, "parseHtml: Truncating HTML from $htmlSize to $MAX_HTML_SIZE for safety")
                    html.take(MAX_HTML_SIZE)
                } else {
                    html
                }
                
                val result = withTimeoutOrNull(PARSE_TIMEOUT_MS) {
                    withContext(Dispatchers.Default) {
                        htmlParserUseCase.parse(safeHtml)
                    }
                } ?: run {
                    Log.e(TAG, "Parsing timed out after ${PARSE_TIMEOUT_MS}ms")
                    _uiState.value = _uiState.value.copy(
                        parseResult = UiText.StringResource(R.string.fetcher_error_timeout),
                        isParsing = false
                    )
                    return@launch
                }

                if (result.questions.isEmpty()) {
                    Log.w(TAG, "No questions found in parsed result!")
                    _uiState.value = _uiState.value.copy(
                        parseResult = UiText.StringResource(R.string.fetcher_error_no_questions),
                        isParsing = false
                    )
                } else {
                    var jsonString = ExtractedQuestionBankSerializer.toJson(result)
                    val jsonSize = jsonString.length
                    
                    if (jsonSize > MAX_RESULT_JSON_SIZE) {
                        Log.w(TAG, "parseHtml: JSON result too large ($jsonSize chars), truncating to ${MAX_RESULT_JSON_SIZE / 1024 / 1024}MB")
                        jsonString = jsonString.take(MAX_RESULT_JSON_SIZE)
                        
                        if (!isValidJson(jsonString)) {
                            Log.e(TAG, "parseHtml: Truncated JSON is invalid, creating minimal structure")
                            jsonString = createMinimalJson(result.questions.size)
                        }
                    }
                    
                    Log.d(TAG, "Parse success! Questions: ${result.questions.size}, JSON size: ${jsonString.length} chars")
                    
                    _uiState.value = _uiState.value.copy(
                        extractedJson = jsonString,
                        navigateToExtract = true,
                        isParsing = false
                    )
                    
                    logMemoryUsage()
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "parseHtml: OOM error during parsing!", e)
                _uiState.value = _uiState.value.copy(
                    parseResult = UiText.StringResource(R.string.fetcher_error_oom),
                    isParsing = false
                )
                
                releaseMemory()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing HTML", e)
                _uiState.value = _uiState.value.copy(
                    parseResult = UiText.StringResource(R.string.fetcher_error_generic, arrayOf(e.message ?: "Unknown error")),
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

    fun saveToSharedDataStore(json: String): Boolean {
        return sharedDataStore.setExtractedJson(json)
    }

    fun onNavigatedToExtract() {
        Log.d(TAG, "onNavigatedToExtract() called")
        _uiState.value = _uiState.value.copy(navigateToExtract = false)
        
        viewModelScope.launch {
            delay(1000)
            val currentJson = _uiState.value.extractedJson
            if (currentJson != null) {
                Log.d(TAG, "onNavigatedToExtract: Clearing extracted JSON (${currentJson.length} chars) to free memory")
                _uiState.value = _uiState.value.copy(extractedJson = null)
                
                releaseMemory()
            }
        }
    }

    private fun isValidJson(json: String): Boolean {
        return try {
            org.json.JSONObject(json)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    private fun createMinimalJson(questionCount: Int): String {
        return try {
            val json = org.json.JSONObject()
            json.put("name", "Partial Result ($questionCount questions)")
            json.put("skippedCount", questionCount)
            json.put("unsupportedTypeCount", 0)
            json.put("questions", org.json.JSONArray())
            json.toString()
        } catch (_: Exception) {
            "{\"name\":\"Partial Result\",\"questions\":[]}"
        }
    }
    
    private fun logMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            Log.d(TAG, "Memory usage: ${usedMemory}MB / ${maxMemory}MB")
            
            if (usedMemory > maxMemory * 0.8) {
                Log.w(TAG, "High memory usage detected! Consider clearing data.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log memory usage", e)
        }
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

    fun decodeHtml(raw: String?): String = htmlParserUseCase.decodeHtmlFromJs(raw)
}
