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
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

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

            viewLifecycleOwner.lifecycleScope.launch {
                SettingsManager.saveDarkMode(requireContext(), isChecked)
            }

            SettingsManager.applyNightMode(isChecked)
        }
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val settingsData = SettingsManager.getSettings(requireContext()).first()

            isInitializing = true
            binding.switchDarkMode.isChecked = settingsData.darkMode
            isInitializing = false
            SettingsManager.applyNightMode(settingsData.darkMode)
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
