package com.example.fisiophone.ui.start

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fisiophone.data.settings.SettingsManager
import com.example.fisiophone.databinding.ActivityPantallaDeInicioBinding
import com.example.fisiophone.ui.auth.LogInActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PantallaDeInicio : AppCompatActivity() {
    private lateinit var binding: ActivityPantallaDeInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedSettings()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPantallaDeInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.iniciarButton.setOnClickListener {
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun applySavedSettings() {
        val settingsData = runBlocking {
            SettingsManager.getSettings(applicationContext).first()
        }

        SettingsManager.applyNightMode(settingsData.darkMode)
    }
}
