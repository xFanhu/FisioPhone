package com.example.fisiophone

import android.app.Application
import com.example.fisiophone.data.settings.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FisioPhoneApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Aplica el tema antes de que se creen las pantallas para evitar cambios visuales al arrancar.
        val settingsData = runBlocking {
            SettingsManager.getSettings(applicationContext).first()
        }

        SettingsManager.applyNightMode(settingsData.darkMode)
    }
}
