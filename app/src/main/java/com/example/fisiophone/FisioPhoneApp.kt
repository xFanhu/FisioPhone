package com.example.fisiophone

import android.app.Application
import com.example.fisiophone.data.settings.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FisioPhoneApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Aplico el tema guardado al arrancar toda la app
        val settingsData = runBlocking {
            SettingsManager.getSettings(applicationContext).first()
        }

        SettingsManager.applyNightMode(settingsData.darkMode)
    }
}