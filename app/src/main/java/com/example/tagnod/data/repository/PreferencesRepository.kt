package com.example.tagnod.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tagnod_settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "DARK", "LIGHT", "SYSTEM"
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color") // "WHITE", "PURPLE", "TEAL", "TURQUOISE"
        val KEY_SHOW_TOAST = booleanPreferencesKey("show_toast_on_trigger")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "DARK"
    }

    val accentColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCENT_COLOR] ?: "WHITE"
    }

    val showToastFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_TOAST] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setAccentColor(colorName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCENT_COLOR] = colorName
        }
    }

    suspend fun setShowToast(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_TOAST] = show
        }
    }
}
