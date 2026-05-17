package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import com.example.fisiophone.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fisiophone.data.settings.SettingsManager
import com.example.fisiophone.databinding.FragmentConfigBinding
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class ConfigFragment : Fragment() {


    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    // Evita disparar el listener al asignar el valor inicial del switch.
    private var isInitializing = true

    private data class Language(val name: String, val code: String)
    private lateinit var availableLanguages: List<Language>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveNotificationSetting(true)
        } else {
            isInitializing = true
            _binding?.switchNotifications?.isChecked = false
            isInitializing = false
            saveNotificationSetting(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        setupLanguageSelector()
        loadSettings()
    }

    private fun setupLanguageSelector() {
        availableLanguages = listOf(
            Language(getString(R.string.idioma_es), "es"),
            Language(getString(R.string.idioma_en), "en")
        )
        
        val adapter = object : android.widget.ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            availableLanguages.map { it.name }
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = availableLanguages.map { it.name }
                        results.count = availableLanguages.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        binding.autoCompleteLanguage.setAdapter(adapter)
    }

    private fun initUI() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            binding.switchDarkMode.isEnabled = false
            val appContext = requireContext().applicationContext

            
            binding.root.postDelayed({
                if (_binding == null) return@postDelayed
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        SettingsManager.saveDarkMode(appContext, isChecked)
                        SettingsManager.applyNightMode(isChecked)
                    } finally {
                        _binding?.switchDarkMode?.isEnabled = true
                    }
                }
            }, 150)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            saveNotificationSetting(true)
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    saveNotificationSetting(true)
                }
            } else {
                saveNotificationSetting(false)
            }
        }

        binding.autoCompleteLanguage.setOnItemClickListener { _, _, position, _ ->
            if (isInitializing) return@setOnItemClickListener

            val selectedLang = availableLanguages[position]
            
            
            binding.root.postDelayed({
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(selectedLang.code)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }, 150)
        }

        binding.cardChangePassword.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.confirmar_cambio_password_titulo))
                .setMessage(getString(R.string.confirmar_cambio_password_mensaje))
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                    dialog.dismiss()
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val email = user?.email
                    if (email != null) {
                        com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (isAdded && context != null) {
                                    if (task.isSuccessful) {
                                        android.widget.Toast.makeText(requireContext(), getString(R.string.email_reset_enviado), android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(requireContext(), task.exception?.message ?: "Error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    }
                }
                .show()
        }
    }

    private fun saveNotificationSetting(enabled: Boolean) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            SettingsManager.saveNotificationsEnabled(appContext, enabled)
        }
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val settingsData = SettingsManager.getSettings(requireContext()).first()
            isInitializing = true
            binding.switchDarkMode.isChecked = settingsData.darkMode
            binding.switchNotifications.isChecked = settingsData.notificationsEnabled
            
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val currentLangCode = if (currentLocales.isEmpty()) "es" else currentLocales[0]?.language ?: "es"
            
            val currentLangName = availableLanguages.find { it.code == currentLangCode }?.name ?: availableLanguages[0].name
            binding.autoCompleteLanguage.setText(currentLangName, false)
            
            isInitializing = false
            binding.switchDarkMode.isEnabled = true
            binding.switchNotifications.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfigFragment"
    }
}
