package mirujam.nekomemo.ui.shared

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedDataStore @Inject constructor() {

    companion object {
        const val MAX_DATA_SIZE = 10 * 1024 * 1024
    }

    private val _extractedJson = MutableStateFlow<String?>(null)

    fun setExtractedJson(json: String): Boolean {
        return try {
            _extractedJson.value = json.take(MAX_DATA_SIZE)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getExtractedJson(): String? {
        return _extractedJson.value
    }

    fun clearExtractedJson(): Boolean {
        return try {
            _extractedJson.value = null
            true
        } catch (_: Exception) {
            false
        }
    }
}
