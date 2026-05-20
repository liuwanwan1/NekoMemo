package mirujam.nekomemo.ui.shared

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val MAX_DATA_SIZE = 10 * 1024 * 1024
        private val EXTRACTED_JSON_KEY = stringPreferencesKey("extracted_json")
    }

    val extractedJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[EXTRACTED_JSON_KEY]
    }

    suspend fun setExtractedJson(json: String): Boolean {
        return try {
            dataStore.edit { preferences ->
                preferences[EXTRACTED_JSON_KEY] = json.take(MAX_DATA_SIZE)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist extracted JSON")
            false
        }
    }

    suspend fun getExtractedJson(): String? {
        return try {
            dataStore.data.first()[EXTRACTED_JSON_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Failed to read extracted JSON")
            null
        }
    }

    suspend fun clearExtractedJson(): Boolean {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(EXTRACTED_JSON_KEY)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear extracted JSON")
            false
        }
    }
}
