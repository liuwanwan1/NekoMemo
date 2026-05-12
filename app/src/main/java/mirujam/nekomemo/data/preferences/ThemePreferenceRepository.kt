package mirujam.nekomemo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Singleton
class ThemePreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val DIRECT_ANSWER_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("direct_answer")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val name = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val directAnswer: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DIRECT_ANSWER_KEY] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    suspend fun setDirectAnswer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIRECT_ANSWER_KEY] = enabled
        }
    }
}
