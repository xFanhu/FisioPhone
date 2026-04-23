package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fisiophone.data.settings.SettingsManager
import com.example.fisiophone.databinding.FragmentConfigBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigFragment : Fragment() {

    // El binding es nullable porque la vista del fragment puede destruirse antes que el fragment.
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    // Evita disparar el listener al asignar el valor inicial del switch.
    private var isInitializing = true

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
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val settingsData = SettingsManager.getSettings(requireContext()).first()
            isInitializing = true
            binding.switchDarkMode.isChecked = settingsData.darkMode
            isInitializing = false
            binding.switchDarkMode.isEnabled = true
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
