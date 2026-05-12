package mirujam.nekomemo.ui.shared

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SharedDataStore {

    private const val PREFS_NAME = "nekomemo_shared_data"
    private const val KEY_EXTRACTED_JSON = "extracted_json"

    private var prefs: SharedPreferences? = null
    private val _extractedJson = MutableStateFlow<String?>(null)
    val extractedJson: StateFlow<String?> = _extractedJson.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                    _extractedJson.value = prefs?.getString(KEY_EXTRACTED_JSON, null)
                }
            }
        }
    }

    fun setExtractedJson(json: String) {
        _extractedJson.value = json
        prefs?.edit()?.putString(KEY_EXTRACTED_JSON, json)?.apply()
    }

    fun getExtractedJson(): String? {
        return _extractedJson.value ?: prefs?.getString(KEY_EXTRACTED_JSON, null)
    }

    fun clearExtractedJson() {
        _extractedJson.value = null
        prefs?.edit()?.remove(KEY_EXTRACTED_JSON)?.apply()
    }
}
