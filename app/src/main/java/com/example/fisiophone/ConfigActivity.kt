package com.example.fisiophone

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.fisiophone.databinding.ActivityConfigBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore de preferencias asociado al contexto
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ConfigActivity : AppCompatActivity() {

    companion object {
        // Clave de la preferencia del modo oscuro
        const val KEY_DARKMODE = "dark_mode"
    }

    // Binding del layout
    private lateinit var binding: ActivityConfigBinding

    // Evita que el listener se ejecute mientras cargamos el valor inicial
    private var isInitializing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializamos binding
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste para barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()
        loadSettings()
    }

    private fun initUI() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            lifecycleScope.launch {
                saveOptions(KEY_DARKMODE, isChecked)
            }

            applyNightMode(isChecked)
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val settingsData = getSettings().first()

            // Mientras cargamos el valor inicial, no queremos que salte el listener
            isInitializing = true
            binding.switchDarkMode.isChecked = settingsData.darkMode
            isInitializing = false
        }
    }

    private suspend fun saveOptions(key: String, value: Boolean) {
        applicationContext.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    private fun getSettings(): Flow<SettingData> =
        applicationContext.dataStore.data.map { preferences ->
            SettingData(
                darkMode = preferences[booleanPreferencesKey(KEY_DARKMODE)] ?: false
            )
        }

    private fun applyNightMode(enabled: Boolean) {
        val newMode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Evita recreaciones innecesarias
        if (AppCompatDelegate.getDefaultNightMode() != newMode) {
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }
}