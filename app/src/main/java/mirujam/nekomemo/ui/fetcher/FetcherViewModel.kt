package mirujam.nekomemo.ui.fetcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mirujam.nekomemo.domain.usecase.HtmlParserUseCase
import mirujam.nekomemo.domain.usecase.decodeHtmlFromJs
import mirujam.nekomemo.ui.model.FetcherUiState
import mirujam.nekomemo.ui.shared.SharedDataStore
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class FetcherViewModel @Inject constructor(
    private val sharedDataStore: SharedDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "FetcherViewModel"
        private const val PARSE_TIMEOUT_MS = 30_000L
        
        // 内存管理常量
        const val MAX_HTML_SIZE = 2 * 1024 * 1024  // HTML最大2MB
        const val MAX_RESULT_JSON_SIZE = 5 * 1024 * 1024  // 结果JSON最大5MB
        const val WARNING_HTML_SIZE = 500_000  // 警告阈值500KB
    }

    private val _uiState = MutableStateFlow(FetcherUiState())
    val uiState: StateFlow<FetcherUiState> = _uiState.asStateFlow()

    // ✅ 优化：使用 StateFlow.map() + stateIn() 替代 mapToField
    val isParsing: StateFlow<Boolean> = _uiState.map { it.isParsing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value.isParsing)
    val parseResult: StateFlow<String?> = _uiState.map { it.parseResult }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value.parseResult)
    val currentUrl: StateFlow<String> = _uiState.map {
        it.currentUrl.ifBlank { "https://i.chaoxing.com" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value.currentUrl.ifBlank { "https://i.chaoxing.com" })
    val navigateToExtract: StateFlow<Boolean> = _uiState.map { it.navigateToExtract }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value.navigateToExtract)

    fun setUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url)
    }

    fun setCurrentUrl(url: String) {
        _uiState.value = _uiState.value.copy(currentUrl = url, urlInput = url)
    }

    fun parseHtml(html: String) {
        // ① 输入验证和大小限制
        if (html.isBlank()) {
            Log.w(TAG, "parseHtml: HTML is blank")
            _uiState.value = _uiState.value.copy(
                parseResult = "Error: Empty HTML content",
                isParsing = false
            )
            return
        }
        
        // ② 检查HTML大小
        val htmlSize = html.length
        when {
            htmlSize > MAX_HTML_SIZE -> {
                val sizeInMB = htmlSize / 1024.0 / 1024.0
                Log.e(TAG, "parseHtml: HTML too large ($htmlSize chars > ${MAX_HTML_SIZE / 1024 / 1024}MB), rejecting")
                _uiState.value = _uiState.value.copy(
                    parseResult = "Error: Page too large (${"%.1f".format(sizeInMB)}MB). Try a smaller page.",
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
                // ③ 截断到安全大小（如果接近限制）
                val safeHtml = if (htmlSize > WARNING_HTML_SIZE) {
                    Log.d(TAG, "parseHtml: Truncating HTML from $htmlSize to $MAX_HTML_SIZE for safety")
                    html.take(MAX_HTML_SIZE)
                } else {
                    html
                }
                
                // ④ 带超时的解析（在后台线程）
                val result = withTimeoutOrNull(PARSE_TIMEOUT_MS) {
                    withContext(Dispatchers.Default) {
                        // 强制GC提示，帮助释放内存
                        System.gc()
                        
                        HtmlParserUseCase.parse(safeHtml)
                    }
                } ?: run {
                    Log.e(TAG, "Parsing timed out after ${PARSE_TIMEOUT_MS}ms")
                    _uiState.value = _uiState.value.copy(
                        parseResult = "Parsing timeout - try a smaller page",
                        isParsing = false
                    )
                    return@launch
                }

                // ⑤ 验证解析结果
                if (result.questions.isEmpty()) {
                    Log.w(TAG, "No questions found in parsed result!")
                    _uiState.value = _uiState.value.copy(
                        parseResult = "No questions found on this page",
                        isParsing = false
                    )
                } else {
                    // ⑥ 转为JSON并检查大小
                    var jsonString = result.toJson()
                    val jsonSize = jsonString.length
                    
                    if (jsonSize > MAX_RESULT_JSON_SIZE) {
                        Log.w(TAG, "parseHtml: JSON result too large ($jsonSize chars), truncating to ${MAX_RESULT_JSON_SIZE / 1024 / 1024}MB")
                        jsonString = jsonString.take(MAX_RESULT_JSON_SIZE)
                        
                        // 如果截断后JSON无效，创建最小有效结构
                        if (!isValidJson(jsonString)) {
                            Log.e(TAG, "parseHtml: Truncated JSON is invalid, creating minimal structure")
                            jsonString = createMinimalJson(result.questions.size)
                        }
                    }
                    
                    Log.d(TAG, "Parse success! Questions: ${result.questions.size}, JSON size: ${jsonString.length} chars")
                    
                    // ⑦ 存储结果（注意：这里会占用内存，后续需要及时清理）
                    _uiState.value = _uiState.value.copy(
                        extractedJson = jsonString,
                        navigateToExtract = true,
                        isParsing = false
                    )
                    
                    // ⑧ 内存使用日志
                    logMemoryUsage()
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "parseHtml: OOM error during parsing!", e)
                _uiState.value = _uiState.value.copy(
                    parseResult = "Error: Out of memory. Page too complex.",
                    isParsing = false
                )
                
                // 尝试释放内存
                releaseMemory()
                System.gc()
                
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

    suspend fun saveToSharedDataStore(json: String): Boolean {
        return sharedDataStore.setExtractedJson(json)
    }

    fun onNavigatedToExtract() {
        Log.d(TAG, "onNavigatedToExtract() called")
        _uiState.value = _uiState.value.copy(navigateToExtract = false)
        
        // 释放内存：导航后清除JSON数据
        viewModelScope.launch {
            delay(1000)  // 延迟1秒确保数据已传递
            val currentJson = _uiState.value.extractedJson
            if (currentJson != null) {
                Log.d(TAG, "onNavigatedToExtract: Clearing extracted JSON (${currentJson.length} chars) to free memory")
                _uiState.value = _uiState.value.copy(extractedJson = null)
                
                // 强制GC
                releaseMemory()
            }
        }
    }
    
    // ========== 内存管理辅助方法 ==========
    
    private fun isValidJson(json: String): Boolean {
        return try {
            org.json.JSONObject(json)
            true
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            "{\"name\":\"Partial Result\",\"questions\":[]}"
        }
    }
    
    private fun logMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            Log.d(TAG, "Memory usage: ${usedMemory}MB / ${maxMemory}MB")
            
            if (usedMemory > maxMemory * 0.8) {  // 超过80%使用率
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

    fun decodeHtml(raw: String?): String = decodeHtmlFromJs(raw)
}
