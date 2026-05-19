package mirujam.nekomemo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val DIRECT_ANSWER_KEY = booleanPreferencesKey("direct_answer")
    }

    val directAnswer: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DIRECT_ANSWER_KEY] ?: false
    }

    suspend fun setDirectAnswer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIRECT_ANSWER_KEY] = enabled
        }
    }
}
