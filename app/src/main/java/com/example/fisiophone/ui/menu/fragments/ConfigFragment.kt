package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
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

class ConfigFragment : Fragment() {

    // El binding es nullable porque la vista del fragment puede destruirse antes que el fragment.
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    // Evita disparar el listener al asignar el valor inicial del switch.
    private var isInitializing = true

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
        loadSettings()
    }

    private fun initUI() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            binding.switchDarkMode.isEnabled = false
            val appContext = requireContext().applicationContext

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SettingsManager.saveDarkMode(appContext, isChecked)
                    SettingsManager.applyNightMode(isChecked)
                } finally {
                    _binding?.switchDarkMode?.isEnabled = true
                }
            }
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
