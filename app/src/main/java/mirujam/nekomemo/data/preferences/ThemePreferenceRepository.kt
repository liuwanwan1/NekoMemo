package mirujam.nekomemo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Singleton
class ThemePreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val name = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            Timber.w(e, "Unknown theme mode: $name, defaulting to SYSTEM")
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }
}
