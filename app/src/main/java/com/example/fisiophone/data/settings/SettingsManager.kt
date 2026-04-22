package com.example.fisiophone.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creo el DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsManager {

    const val KEY_DARKMODE = "dark_mode"

    // Clave para el modo oscuro
    private val darkModeKey = booleanPreferencesKey(KEY_DARKMODE)

    // Obtengo los ajustes guardados
    fun getSettings(context: Context): Flow<SettingData> =
        context.applicationContext.dataStore.data.map { preferences ->
            SettingData(
                darkMode = preferences[darkModeKey] ?: false
            )
        }

    // Guardo el valor del modo oscuro
    suspend fun saveDarkMode(context: Context, enabled: Boolean) {
        context.applicationContext.dataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }

    // Aplico el modo oscuro o claro
    fun applyNightMode(enabled: Boolean) {
        val newMode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Solo lo cambio si hace falta
        if (AppCompatDelegate.getDefaultNightMode() != newMode) {
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }
}