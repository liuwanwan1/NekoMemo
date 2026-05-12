package mirujam.nekomemo.ui.fetcher

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FetcherViewModel @Inject constructor() : ViewModel() {

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parseResult = MutableStateFlow<String?>(null)
    val parseResult: StateFlow<String?> = _parseResult.asStateFlow()

    private val _urlInput = MutableStateFlow("https://i.chaoxing.com")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _currentUrl = MutableStateFlow("https://i.chaoxing.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _navigateToExtract = MutableStateFlow(false)
    val navigateToExtract: StateFlow<Boolean> = _navigateToExtract.asStateFlow()

    private var _webView: WebView? = null

    fun getOrCreateWebView(factory: () -> WebView): WebView {
        if (_webView == null) {
            _webView = factory()
        }
        return _webView!!
    }

    override fun onCleared() {
        super.onCleared()
        _webView?.destroy()
        _webView = null
    }

    fun setUrlInput(url: String) {
        _urlInput.value = url
    }

    fun setCurrentUrl(url: String) {
        _currentUrl.value = url
        _urlInput.value = url
    }

    fun parseHtml(html: String) {
        viewModelScope.launch {
            _isParsing.value = true
            _parseResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    HtmlParser.parse(html)
                }
                if (result.questions.isEmpty()) {
                    _parseResult.value = "No questions found on this page"
                } else {
                    ExtractedDataCache.bank = result
                    _navigateToExtract.value = true
                }
            } catch (e: Exception) {
                _parseResult.value = "Error: ${e.message}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun onNavigatedToExtract() {
        _navigateToExtract.value = false
    }

    fun clearResult() {
        _parseResult.value = null
    }
}
